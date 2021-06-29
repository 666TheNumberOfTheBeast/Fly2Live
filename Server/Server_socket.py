# ********* ABOUT *********
# Server for handling a single game at time
# (2 players play, the others wait the server resources are free)
# *************** *********

from flask import Flask, request
from flask_socketio import SocketIO, send, emit, join_room, leave_room

#from pygame import Rect

from Player   import *
from Obstacle import *

import random
import time


# Constants for the server state
STATE_INIT    = 0
STATE_SEARCH  = 1
STATE_PREPARE = 2
STATE_READY   = 3
STATE_PLAY    = 4
STATE_END     = 5

# Constant for the number of players in a game
NUMBER_PLAYERS_GAME = 2

# Constants for the game end status (i.e. winner)
UNDEFINED         = -1
WINNER_PLAYER_0   = 0
WINNER_PLAYER_1   = 1
WINNER_DRAW       = 2

# Constants for the client requests
REQ_CODE_NEW_GAME = 0
REQ_CODE_NEW_MOVE = 1
#REQ_CODE_ = 2

# Constants for the messages
MSG_CODE_BAD_REQ       = 0
MSG_CODE_SEARCHING_ADV = 1
MSG_CODE_FOUND_ADV     = 2
MSG_CODE_PREPARE       = 3
MSG_CODE_GAME_START    = 4
MSG_CODE_GAME_READY    = 5
MSG_CODE_GAME_PLAY     = 6
MSG_CODE_GAME_END      = 7
MSG_CODE_SERVER_BUSY   = 8
MSG_CODE_RESEND        = 9

# Constants for the game scenarios
SCENARIO_CITY_DAY   = 0
SCENARIO_CITY_NIGHT = 1

INITIAL_SPEED = 10.0


# Variables of the server for keeping the game state
state = STATE_INIT

# Dictionary of players currently connected to the server
players_connected = {}
# List of players currently in the room
players_in_room = []
# Dictionary of players in the room that are ready after game start (similar to ACKs)
players_ready = {}

# For using multiple rooms, I need multithread for properly handling below server variables
#rooms   = []
room = "game"

scenario = UNDEFINED

player_0 = None
player_1 = None
cpu_building = None
cpu_vehicle  = None

world_width  = 55.0  # In meters
#world_height = 60.0  # In meters
speed = INITIAL_SPEED # In m/s


player_bitmap_bounds_offsets = [
    [0.33, 0.2, 0.64, 0.76], # Bottom rect
    [0.07, 0.2, 0.33, 0.5],  # Middle rect
    [0.64, 0.19, 0.95, 0.21] # Top rect
]
player_bitmap_width  = 10.0  # In meters
player_bitmap_height = 3.0   # In meters
player_initial_pos_x = 1.0   # In meters
player_initial_pos_y = 20.0  # In meters
#player_initial_pos_x = 0.02  # In width ratio
#player_initial_pos_y = 0.4   # In height ratio

obstacle_initial_pos_x = world_width + 3.0  # In meters
obstacle_initial_pos_y = 40.0               # In meters
# Use Width and Height ratio to be universal
#obstacle_initial_pos_x = 1.02  # In width ratio
#obstacle_initial_pos_y = 0.5   # In height ratio

score  = 0.0
winner = UNDEFINED

# Width and height ratios normalized in [0,1]
# NB: x goes from 0 to width  = from left to right = from 0 to 1
# NB: y goes from 0 to height = from top to bottom = from 0 to 1
# 10/5 = 2 => 5/10 = 0.5

buildings = UNDEFINED

buildings_day = [
    Obstacle(
        "building_01",
        [
            [0.0, 0.048, 1.0, 1.0],   # Bottom rect
            [0.07, 0.031, 0.38, 1.0], # Middle left rect
            [0.47, 0.031, 0.8, 1.0],  # Middle right rect
            [0.1, 0.0, 0.18, 1.0],    # Top left rect
            [0.47, 0.023, 0.63, 1.0]  # Top right rect
        ],
        35.0, 130.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_02",
        [
            [0.18, 0.365, 0.82, 1.0], # Bottom rect
            [0.33, 0.187, 0.7, 1.0],  # Middle rect
            [0.47, 0.0, 0.56, 1.0]    # Top rect
        ],
        18.0, 140.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_03",
        [
            [0.0, 0.16, 1.0, 1.0],    # Bottom rect
            [0.13, 0.105, 0.86, 1.0], # Middle rect 1
            [0.28, 0.045, 0.68, 1.0], # Middle rect 2
            [0.45, 0.0, 0.6, 1.0]     # Top left rect
        ],
        28.0, 150.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_04",
        [
            [0.13, 0.28, 0.86, 1.0],  # Bottom rect
            [0.21, 0.173, 0.79, 1.0], # Middle rect 1
            [0.23, 0.07, 0.76, 1.0],  # Middle rect 2
            [0.31, 0.003, 0.67, 1.0], # Top rect
        ],
        53.0, 120.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_05",
        [
            [0.0, 0.185, 1.0, 1.0],   # Bottom rect
            [0.0, 0.1, 0.23, 1.0],    # Middle left rect
            [0.27, 0.136, 0.75, 1.0], # Middle center rect
            [0.77, 0.136, 0.9, 1.0],  # Middle right rect
            [0.33, 0.0, 0.75, 1.0]    # Top rect
        ],
        80.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_06",
        [
            [0.0, 0.55, 1.0, 1.0],   # Bottom rect
            [0.0, 0.14, 0.36, 1.0],  # Middle left rect
            [0.53, 0.32, 0.86, 1.0], # Middle right rect
            [0.07, 0.0, 0.29, 1.0]   # Top rect
        ],
        160.0, 80.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_07",
        [
            [0.0, 0.291, 1.0, 1.0],  # Bottom rect
            [0.0, 0.15, 0.91, 1.0],  # Middle rect
            [0.0, 0.13, 0.22, 1.0],  # Left triangle
            [0.7, 0.13, 0.91, 1.0],  # Right triangle
            [0.31, 0.06, 0.39, 1.0], # Top rect 1
            [0.31, 0.0, 0.33, 1.0]   # Top rect 2
        ],
        30.0, 120.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_08",
        [
            [0.0, 0.123, 1.0, 1.0], # Bottom rect
            [0.1, 0.0, 0.9, 1.0]    # Top rect
        ],
        28.0, 120.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_09",
        [
            [0.0, 0.115, 1.0, 1.0],   # Bottom rect
            [0.28, 0.095, 0.7, 1.0],  # Middle rect 1
            [0.4, 0.026, 0.5, 1.0],   # Middle rect 2
            [0.43, 0.003, 0.48, 1.0]  # Top rect
        ],
        40.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_10",
        [
            [0.0, 0.094, 1.0, 1.0],   # Bottom rect
            [0.09, 0.087, 0.96, 1.0], # Middle rect
            [0.13, 0.015, 0.29, 1.0], # Top left rect
            [0.4, 0.0, 0.7, 1.0],     # Top middle rect
            [0.75, 0.026, 0.92, 1.0]  # Top right rect
        ],
        30.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_11",
        [
            [0.0, 0.03, 1.0, 1.0],   # Bottom rect
            [0.15, 0.0, 0.57, 1.0]  # Top rect
        ],
        35.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_12",
        [
            [0.0, 0.042, 1.0, 1.0], # Bottom rect
            [0.06, 0.0, 0.9, 1.0]   # Top rect
        ],
        30.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_13",
        [
            [0.0, 0.11, 1.0, 1.0],   # Bottom rect
            [0.05, 0.01, 0.15, 1.0], # Top left rect
            [0.15, 0.0, 0.82, 1.0],  # Top middle rect
            [0.83, 0.04, 1.0, 1.0],  # Top right rect 1
            [0.86, 0.0, 0.94, 1.0]   # Top right rect 2
        ],
        45.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_14",
        [
            [0.0, 0.185, 1.0, 1.0],   # Bottom rect
            [0.29, 0.065, 0.68, 1.0], # Middle rect
            [0.53, 0.035, 0.59, 1.0], # Top left rect
            [0.34, 0.0, 0.35, 1.0]    # Top right rect
        ],
        55.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_15",
        [
            [0.0, 0.43, 1.0, 1.0],    # Bottom rect
            [0.1, 0.3, 0.86, 1.0],    # Middle rect 1
            [0.25, 0.256, 0.71, 1.0], # Middle rect 2
            [0.28, 0.236, 0.68, 1.0], # Middle rect 3
            [0.4, 0.18, 0.57, 1.0],   # Top rect 1
            [0.53, 0.0, 0.57, 1.0]    # Top rect 2
        ],
        25.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_16",
        [
            [0.0, 0.2, 1.0, 1.0],     # Bottom rect
            [0.13, 0.175, 0.87, 1.0], # Middle rect 1
            [0.24, 0.147, 0.76, 1.0], # Middle rect 2
            [0.28, 0.122, 0.49, 1.0], # Middle rect 3
            [0.53, 0.0, 0.55, 1.0],   # Top left rect
            [0.6, 0.039, 0.62, 1.0],  # Top middle rect
            [0.68, 0.03, 0.71, 1.0]   # Top right rect
        ],
        40.0, 120.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_17",
        [
            [0.0, 0.094, 1.0, 1.0],  # Bottom rect
            [0.15, 0.06, 0.88, 1.0], # Middle rect
            [0.41, 0.0, 0.63, 1.0]   # Top rect
        ],
        50.0, 120.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    )
]

buildings_night = [
    Obstacle(
        "building_01",
        [
            [0.0, 0.19, 1.0, 1.0],   # Bottom rect
            [0.05, 0.175, 1.0, 1.0],  # Bottom rect
            [0.05, 0.151, 0.92, 1.0], # Middle rect 1
            [0.85, 0.135, 0.92, 1.0], # Middle rect 1
            [0.1, 0.12, 0.86, 1.0],   # Middle rect 2
            [0.78, 0.105, 0.86, 1.0], # Middle rect 2
            [0.2, 0.087, 0.78, 1.0],  # Top rect
            [0.25, 0.077, 0.72, 1.0], # Top rect
            [0.3, 0.067, 0.65, 1.0],  # Top rect
            [0.35, 0.06, 0.6, 1.0]    # Top rect
        ],
        22.0, 140.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_02",
        [
            [0.26, 0.23, 0.72, 1.0], # Bottom rect
            [0.4, 0.1, 0.6, 1.0],    # Middle rect
            [0.45, 0.0, 0.54, 1.0]   # Top rect
        ],
        13.0, 130.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_03",
        [
            [0.75, 0.072, 1.0, 1.0], # Bottom rect
            [0.0, 0.045, 0.75, 1.0], # Bottom rect
            [0.68, 0.02, 0.82, 1.0], # Top rect
            [0.16, 0.0, 0.68, 1.0]   # Top rect
        ],
        32.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_04",
        [
            [0.0, 0.11, 0.24, 1.0],  # Bottom rect
            [0.24, 0.085, 1.0, 1.0], # Bottom rect
            [0.16, 0.02, 0.3, 1.0],  # Top rect
            [0.3, 0.0, 0.82, 1.0]    # Top rect
        ],
        32.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_05",
        [
            [0.0, 0.09, 1.0, 1.0],  # Bottom rect
            [0.0, 0.031, 0.7, 1.0], # Middle rect 1
            [0.45, 0.01, 0.6, 1.0], # Middle rect 2
            [0.7, 0.05, 0.8, 1.0],  # Middle rect 3
            [0.8, 0.07, 0.9, 1.0],  # Middle rect 4
            [0.13, 0.0, 0.45, 1.0]  # Top rect
        ],
        50.0, 130.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_06",
        [
            [0.0, 0.11, 1.0, 1.0],    # Bottom rect
            [0.05, 0.09, 0.95, 1.0],  # Middle rect 1
            [0.1, 0.07, 0.9, 1.0],    # Middle rect 2
            [0.15, 0.057, 0.86, 1.0], # Middle rect 3
            [0.18, 0.048, 0.82, 1.0], # Middle rect 4
            [0.23, 0.04, 0.76, 1.0],  # Middle rect 5
            [0.28, 0.03, 0.72, 1.0],  # Middle rect 6
            [0.33, 0.02, 0.66, 1.0],  # Middle rect 7
            [0.37, 0.015, 0.63, 1.0], # Middle rect 8
            [0.41, 0.009, 0.59, 1.0], # Middle rect 9
            [0.47, 0.0, 0.52, 1.0]    # Top rect
        ],
        40.0, 130.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_07",
        [
            [0.0, 0.09, 0.93, 1.0],  # Bottom rect
            [0.06, 0.05, 0.87, 1.0], # Middle rect
            [0.19, 0.01, 0.74, 1.0], # Middle rect
            [0.26, 0.0, 0.67, 1.0]   # Top rect
        ],
        30.0, 120.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_08",
        [
            [0.0, 0.02, 1.0, 1.0],   # Bottom rect
            [0.1, 0.015, 0.95, 1.0], # Middle rect 1
            [0.27, 0.01, 0.9, 1.0],  # Middle rect 2
            [0.47, 0.05, 0.8, 1.0],  # Middle rect 3
            [0.65, 0.0, 0.75, 1.0]   # Top rect
        ],
        30.0, 120.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_09",
        [
            [0.0, 0.022, 1.0, 1.0],   # Bottom rect
            [0.08, 0.015, 0.93, 1.0], # Middle rect 1
            [0.15, 0.008, 0.87, 1.0], # Middle rect 2
            [0.23, 0.0, 0.77, 1.0]    # Top rect
        ],
        55.0, 140.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_10",
        [
            [0.25, 0.029, 1.0, 1.0], # Bottom rect
            [0.33, 0.02, 0.87, 1.0]  # Top right rect
        ],
        25.0, 120.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "building_11",
        [
            [0.0, 0.183, 1.0, 1.0], # Bottom rect
            [0.1, 0.16, 0.9, 1.0],   # Middle rect
            [0.2, 0.14, 0.8, 1.0],   # Middle rect
            [0.3, 0.125, 0.7, 1.0],  # Middle rect
            [0.4, 0.09, 0.54, 1.0],  # Middle rect
            [0.46, 0.0, 0.5, 1.0]    # Top rect
        ],
        18.0, 130.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    )
]

obstacle_initial_pos_y = 20.0  # In meters
#obstacle_initial_pos_y = 0.05  # In height ratio

vehicles = [
    Obstacle(
        "vehicle_01",
        [
            [0.0, 0.3, 0.2, 1.0],  # Bottom rect
            [0.0, 0.3, 1.0, 0.77], # Middle rect
            [0.1, 0.0, 0.4, 0.6]   # Top rect
        ],
        6.0, 2.5, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "vehicle_02",
        [
            [0.27, 0.77, 0.57, 1.0],  # Bottom rect
            [0.0, 0.02, 1.0, 0.6],    # Middle rect 1
            [0.08, 0.1, 0.75, 0.7],   # Middle rect 2
            [0.15, 0.01, 0.65, 0.78], # Middle rect 3
            [0.75, 0.0, 0.93, 0.78]   # Top rect
        ],
        20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "vehicle_03",
        [
            [0.24, 0.8, 0.4, 1.0],  # Bottom rect
            [0.0, 0.05, 1.0, 0.85], # Middle rect
            [0.77, 0.0, 1.0, 0.92]  # Top rect
        ],
        20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "vehicle_04",
        [
            [0.28, 0.8, 0.44, 1.0],   # Bottom rect
            [0.0, 0.3, 1.0, 0.6],     # Middle rect 1
            [0.05, 0.2, 0.15, 0.7],   # Middle rect 2
            [0.15, 0.06, 0.77, 0.85], # Middle rect 3
            [0.77, 0.0, 0.92, 0.9]    # Top rect
        ],
        20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "vehicle_05",
        [
            [0.29, 0.8, 0.44, 1.0],  # Bottom rect
            [0.0, 0.3, 1.0, 0.53],   # Middle rect 1
            [0.05, 0.1, 0.86, 0.7],  # Middle rect 2
            [0.12, 0.0, 0.77, 0.85], # Middle rect 3
            [0.86, 0.0, 0.97, 0.85]  # Top rect
        ], 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "vehicle_06",
        [
            [0.29, 0.8, 0.51, 1.0],  # Bottom rect
            [0.0, 0.3, 1.0, 0.58],   # Middle rect 1
            [0.05, 0.1, 0.86, 0.76], # Middle rect 2
            [0.12, 0.0, 0.77, 0.85], # Middle rect 3
            [0.86, 0.12, 0.97, 0.8]  # Top rect
        ], 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "vehicle_07",
        [
            [0.26, 0.8, 0.49, 1.0],  # Bottom rect
            [0.0, 0.3, 0.82, 0.58],  # Middle rect 1
            [0.05, 0.1, 0.71, 0.76], # Middle rect 2
            [0.12, 0.0, 0.6, 0.85],  # Middle rect 3
            [0.82, 0.17, 0.99, 0.7]  # Top rect
        ], 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "vehicle_08",
        [
            [0.32, 0.8, 0.48, 1.0],  # Bottom rect
            [0.0, 0.25, 0.07, 0.65], # Middle rect 1
            [0.1, 0.04, 0.82, 0.82], # Middle rect 2
            [0.82, 0.02, 0.99, 0.86] # Top rect
        ], 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    ),
    Obstacle(
        "vehicle_09",
        [
            [0.0, 0.0, 1.0, 1.0]
        ], 4.2, 2.1, obstacle_initial_pos_x, obstacle_initial_pos_y, speed
    )
]

# NB: The previous widths and heights of obstacles must be consistent with the client


app = Flask(__name__)
app.config["SECRET_KEY"] = "my_secret_key"
#socketio = SocketIO(app)

# Monkey patching is necessary to emit messages from a background thread
socketio = SocketIO(app, async_mode="eventlet")
import eventlet
eventlet.monkey_patch()



# ********* APIs *********
# TODO: bind each user to a UID (with Google login is not necessary, use the Google one)
#@app.route('/', methods=['GET'])
#def home():
#    return "<h1>Distant Reading Archive</h1><p>This site is a prototype API for distant reading of science fiction novels.</p>"
# ************************



# Event connection to server
@socketio.on("connect")
def connect():
    # No need global variables declaration to read but not modify them
    print("=========================")
    print("Client connected: " + request.sid)

    # Add the player socket ID to the dictionary of players currently connected to the server
    players_connected[request.sid] = { "id": None, "room": None, "scenario": None }

    print("Players connected to the server: ", players_connected)


# Event join a room
'''@socketio.on('join')
def on_join(data):
    username = data['username']
    room = data['room']
    join_room(room)
    send(username + ' has entered the room.', to=room)'''


# Event disconnect from server
@socketio.on("disconnect")
def disconnect():
    # Declare global variables that will be modified by this function
    global state, \
           player_0, player_1, \
           cpu_building, cpu_vehicle, \
           score, winner

    print("=========================")
    print("Client disconnected: " + request.sid)

    # Remove the player from the dictionary of players connected to the server
    player_id = players_connected.pop(request.sid)["id"]
    print("Player ID: ", player_id)
    print("Server state: ", state)

    # Check if player quit before sending NEW_GAME request or
    # if he quit while is waiting in the dictionary of connected players
    if not player_id or request.sid not in players_in_room:
        return

    # Else the player who quit is in the room, so remove him from there
    leave_room(room, sid=request.sid)
    players_in_room.remove(request.sid)

    # Check if player_0 quit while the server is in the STATE_SEARCH
    if state == STATE_SEARCH and player_0 and player_id == player_0.getId():
        player_0 = None
        state = STATE_INIT

        # Check if there is a player_1 (it should not happen, as the server should be in >=STATE_PREPARE)
        if player_1:
            # Try to notify someone connected to the server who shares the same scenario
            __notifyPlayerByScenario(scenario, player_1.getId())
        else:
             # Try to notify two players connected to the server who share the same scenario
            __notifyPlayerConnected()

    # Check if player_1 quit while the server is in the STATE_SEARCH
    elif state == STATE_SEARCH and player_1 and player_id == player_1.getId():
        player_1 = None
        state = STATE_INIT

        # Check if there is a player_0 (it should not happen, as the server should be in >=STATE_PREPARE)
        if player_0:
            # Try to notify someone connected to the server who shares the same scenario
            __notifyPlayerByScenario(scenario, player_0.getId())
        else:
             # Try to notify two players connected to the server who share the same scenario
            __notifyPlayerConnected()

    # Check if player_0 quit while the server is in the STATE_PREPARE
    elif state == STATE_PREPARE and player_id == player_0.getId():
        player_0 = None
        state = STATE_SEARCH
        emit("new game response",
            { "error": False,
              "msg_code": MSG_CODE_SEARCHING_ADV
            }, to=room)

        # Try to notify someone connected to the server who shares the same scenario
        __notifyPlayerByScenario(scenario, player_1.getId())

    # Check if player_1 quit while the server is in the STATE_PREPARE
    elif state == STATE_PREPARE and player_id == player_1.getId():
        player_1 = None
        state = STATE_SEARCH
        emit("new game response",
            { "error": False,
              "msg_code": MSG_CODE_SEARCHING_ADV
            }, to=room)

        # Try to notify someone connected to the server who shares the same scenario
        __notifyPlayerByScenario(scenario, player_0.getId())

    # Check if player_0 quit while the server is in the STATE_READY or STATE_PLAY
    elif (state == STATE_READY or state == STATE_PLAY) and player_id == player_0.getId():
        if state == STATE_PLAY:
            winner = WINNER_PLAYER_1
        state = STATE_END
        emit("game update",
            { "error": False,
              "msg_code": MSG_CODE_GAME_END,
              "cpu_building": { "id": cpu_building.getId(),
                                "pos_x": cpu_building.getX(),
                                "pos_y": cpu_building.getY()#,
                                #"width": cpu_building.getWidth(),
                                #"height": cpu_building.getHeight()
                              },
              "cpu_vehicle": { "id": cpu_vehicle.getId(),
                               "pos_x": cpu_vehicle.getX(),
                               "pos_y": cpu_vehicle.getY()#,
                               #"width": cpu_vehicle.getWidth(),
                               #"height": cpu_vehicle.getHeight()
                             },
              "player_0": { "id": player_0.getId(),
                            "pos_x": player_0.getX(),
                            "pos_y": player_0.getY(),
                            "rotation": player_0.getRotation()#,
                            #"width": player_0.getBitmapWidth(),
                            #"height": player_0.getBitmapHeight()'''
                          },
              "player_1": { "id": player_1.getId(),
                            "pos_x": player_1.getX(),
                            "pos_y": player_1.getY(),
                            "rotation": player_1.getRotation()#,
                            #"width": player_1.getBitmapWidth(),
                            #"height": player_1.getBitmapHeight()
                          },
              "score": score,
              "winner": winner
            }, to=room)

    # Check if player_1 quit while the server is in the STATE_READY or STATE_PLAY
    elif (state == STATE_READY or state == STATE_PLAY) and player_id == player_1.getId():
        if state == STATE_PLAY:
            winner = WINNER_PLAYER_0
        state = STATE_END
        emit("game update",
            { "error": False,
              "msg_code": MSG_CODE_GAME_END,
              "cpu_building": { "id": cpu_building.getId(),
                                "pos_x": cpu_building.getX(),
                                "pos_y": cpu_building.getY()#,
                                #"width": cpu_building.getWidth(),
                                #"height": cpu_building.getHeight()
                              },
              "cpu_vehicle": { "id": cpu_vehicle.getId(),
                               "pos_x": cpu_vehicle.getX(),
                               "pos_y": cpu_vehicle.getY()#,
                               #"width": cpu_vehicle.getWidth(),
                               #"height": cpu_vehicle.getHeight()
                             },
              "player_0": { "id": player_0.getId(),
                            "pos_x": player_0.getX(),
                            "pos_y": player_0.getY(),
                            "rotation": player_0.getRotation()#,
                            #"width": player_0.getBitmapWidth(),
                            #"height": player_0.getBitmapHeight()'''
                          },
              "player_1": { "id": player_1.getId(),
                            "pos_x": player_1.getX(),
                            "pos_y": player_1.getY(),
                            "rotation": player_1.getRotation()#,
                            #"width": player_1.getBitmapWidth(),
                            #"height": player_1.getBitmapHeight()
                          },
              "score": score,
              "winner": winner
            }, to=room)

    # Check if all player have been disconnected after being received game end
    elif state == STATE_END and not players_in_room:
        # NB: At this point, NUMBER_PLAYERS_GAME players are disconnected from the server and
        # removed from the dictionary of connected players

        # Try to notify two players connected to the server who share the same scenario
        __notifyPlayerConnected()



# Notify a player to resend the new game request
def __notify(player_sid):
    if not player_sid:
        return

    emit("new game response",
         { "error": False,
           "msg_code": MSG_CODE_RESEND
         }, to=player_sid )

    print("Resend request new game sent to client: ", player_sid)

# Get a player by scenario among connected ones with a different ID from player_id
def __getPlayerByScenario(scenario, player_id):
    print("Get player by scenario: ", scenario)

    for player_sid, player_info in players_connected.items():
        print("player id: ", player_info["id"])
        print("player scenario: ", player_info["scenario"])

        if player_info["id"] != player_id and player_info["scenario"] == scenario:
            return player_sid  # Player socket ID
    return None

# Get a player by scenario among connected ones with a different ID from player_id and
# notify it to resend a new game request
def __notifyPlayerByScenario(scenario, player_id):
    player_sid = __getPlayerByScenario(scenario, player_id)
    __notify(player_sid)

def __notifyPlayerConnected():
    global state

    print("Try to notify two players connected to the server who share the same scenario...")

    # Check if there are other players connected to server
    if not players_connected:
        print("Nobody is connected to the server")
        # Nobody is connected, so go to STATE_INIT
        state = STATE_INIT
        return

    # Check if there is only one player connected to server
    if len(players_connected) == 1:
        print("One player is connected to the server, notify him")
        # There is one, so go to STATE_INIT
        state = STATE_INIT

        # Get his socket id and
        # tell him to resend a new game message to restart the process
        for player in players_connected:
            __notify(player)
            '''with app.app_context():
                socketio.emit("new game response",
                     { "error": False,
                       "msg_code": MSG_CODE_RESEND
                     }, to=player )

                print("Resend request new game sent to client: ", player)'''
            return

    # Else, select two players from the dictionary of connected players
    # that share the same scenario (VERSION WITH NUMBER_PLAYERS_GAME = 2)
    players_by_scenarios = {}
    player_sid = None

    for player_sid, player_info in players_connected.items():
        print("player id: ", player_info["id"])
        print("player scenario: ", player_info["scenario"])

        s = player_info["scenario"]

        # Check if the scenario has been encountered so far
        if s in players_by_scenarios:
            # There is one entry, get the other player sid and
            # tell them to resend a new game message to restart the process
            print("There are two players who share the same scenario, notify them")

            # Go to STATE_INIT
            state = STATE_INIT

            __notify(player_sid)
            __notify(players_by_scenarios[s])

            '''with app.app_context():
                socketio.emit("new game response",
                     { "error": False,
                       "msg_code": MSG_CODE_RESEND
                     }, to=player )

                socketio.emit("new game response",
                     { "error": False,
                       "msg_code": MSG_CODE_RESEND
                     }, to=players_by_scenarios[s] )

                print("Resend request new game sent to client: ", player)
                print("Resend request new game sent to client: ", players_by_scenarios[s])'''

            return

        # Else check if the scenario is not None
        elif s:
            # Add an entry in the dictionary indexed by scenarios
            players_by_scenarios[s] = player_sid  # Player socket ID

    print("There are not two players who share the same scenario, notify one player among connected ones")
    # There are not NUMBER_PLAYERS_GAME players that share the same scenario,
    # notify one random (VERSION WITH NUMBER_PLAYERS_GAME = 2)
    __notify(player_sid)


# Event new game request
@socketio.on("new game request")
def new_game_event(json):
    global state, \
           room, players_in_room, \
           scenario, buildings, \
           player_0, player_1, \
           cpu_building, cpu_vehicle, \
           score, winner, \
           players_ready

    # Validate input and return True if it is ok
    def __validate(input):
        if not "who" in input or not isinstance(input["who"], str):
            return False
        if not "req" in input or not isinstance(input["req"], int):
            return False
        if not "screen_width" in input or not isinstance(input["screen_width"], int) or input["screen_width"] <= 0:
            return False
        if not "screen_height" in input or not isinstance(input["screen_height"], int) or input["screen_height"] <= 0:
            return False
        if not "scenario" in input or not isinstance(input["scenario"], int) or input["scenario"] < 0:
            return False
        return True

    # Find an available room, put the player in it and return the room
    def __addPlayerToRoom():
        # Check if a room is available (multithread version)
        '''if not rooms:
            room = request.sid
            rooms.append(room)
        else:
            room = rooms.pop(0)'''

        # Check if the room is available (monothread version)
        if len(players_in_room) == NUMBER_PLAYERS_GAME:
            # Room is busy
            return None

        print("Player added to the room: " + room)
        # Add the player to an available room
        join_room(room, sid=request.sid)
        players_in_room.append(request.sid)

        return room

    # Add player ID, room and scenario
    # to the corresponding entry in the dictionary of players connected to the server
    def __addPlayerInfo(id, room, scenario):
        players_connected[request.sid]["id"]       = id
        players_connected[request.sid]["room"]     = room
        players_connected[request.sid]["scenario"] = scenario


    print("=========================")
    print("Received new game event: " + str(json))
    print("Client: " + request.sid)
    print("Server state: ", state)

    # Check if input is valid
    if not __validate(json):
        emit("new game response", { "error": True, "msg_code": MSG_CODE_BAD_REQ })
        return

    # Check if a NEW_GAME request arrives while the server is in the STATE_INIT
    if state == STATE_INIT and json["req"] == REQ_CODE_NEW_GAME:
        # Reset players in room
        players_in_room = []

        room = __addPlayerToRoom()

        # Fill player entry in the dictionary of players connected to the server
        __addPlayerInfo(json["who"], room, json["scenario"])

        # Reset last players IDs variables
        player_0 = Player(
            json["who"], json["screen_width"], json["screen_height"], world_width,
            player_bitmap_bounds_offsets, player_bitmap_width, player_bitmap_height,
            player_initial_pos_x, player_initial_pos_y, 0.0)
        player_1 = None

        # Reset obstacles variables
        cpu_building = None
        cpu_vehicle  = None

        # Reset winner, score & speed
        winner = UNDEFINED
        score = 0.0
        speed = INITIAL_SPEED

        # Reset scenario & buildings
        scenario = json["scenario"]
        buildings = buildings_day if scenario == SCENARIO_CITY_DAY else buildings_night

        # Reset dictionary of players ready to play
        players_ready = {}

        # Go to next server state
        state = STATE_SEARCH
        emit("new game response", { "error": False, "msg_code": MSG_CODE_SEARCHING_ADV })

    # Check if NEW_GAME request while the server is in the STATE_SEARCH
    elif state == STATE_SEARCH and json["req"] == REQ_CODE_NEW_GAME:
        # Check if the request is coming from the same player who is waiting for an adversary.
        # NEW_GAME request can come from player_1 if the previous player_0 quit during STATE_PREPARE
        if (player_0 and json["who"] == player_0.getId()) or (player_1 and json["who"] == player_1.getId()):
            emit("new game response", { "error": False, "msg_code": MSG_CODE_SEARCHING_ADV })

        # Otherwise NEW_GAME request is coming from a different player than before
        else:
            # Check if players scenarios are equal
            if not json["scenario"] == scenario:
                emit("new game response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })

                # Fill player entry in the dictionary of players connected to the server
                __addPlayerInfo(json["who"], None, json["scenario"])
                return

            # Check if player_0 is free
            if not player_0:
                player_0 = Player(
                    json["who"], json["screen_width"], json["screen_height"], world_width,
                    player_bitmap_bounds_offsets, player_bitmap_width, player_bitmap_height,
                    player_initial_pos_x, player_initial_pos_y, 0.0)
            # Check if player_1 is free
            elif not player_1:
                player_1 = Player(
                    json["who"], json["screen_width"], json["screen_height"], world_width,
                    player_bitmap_bounds_offsets, player_bitmap_width, player_bitmap_height,
                    player_initial_pos_x, player_initial_pos_y, 0.0)
            else:
                # Just for super-safety but this condition should never be true
                # because the server should be in STATE_PREPARE
                emit("new game response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })

                # Fill player entry in the dictionary of players connected to the server
                __addPlayerInfo(json["who"], None, json["scenario"])
                return

            # Go to next server state
            state = STATE_PREPARE

            room = __addPlayerToRoom()
            # Fill player entry in the dictionary of players connected to the server
            __addPlayerInfo(json["who"], room, json["scenario"])

            # Send to the two players in game not to all (there could be also people waiting)
            emit("new game response", { "error": False, "msg_code": MSG_CODE_FOUND_ADV }, to=room)

            cpu_building = __pickBuilding()
            cpu_vehicle  = __pickVehicle()

            print("cpu_building ID: " + cpu_building.getId())
            print("cpu_vehicle ID: " + cpu_vehicle.getId())

            # Check if a player quit while server state was in STATE_PREPARE
            if (state != STATE_PREPARE):
                # Send to the remaining player in the room
                emit("new game response", { "error": False, "msg_code": MSG_CODE_SEARCHING_ADV }, to=room)
                return

            # Go to next server state
            state = STATE_READY

            emit("new game response",
                { "error": False,
                  "msg_code": MSG_CODE_GAME_START
                }, to=room )

    # Check if NEW_GAME request while the server is in the STATE_PREPARE
    elif state == STATE_PREPARE and json["req"] == REQ_CODE_NEW_GAME:
        if json["who"] == player_0.getId() or json["who"] == player_1.getId():
            emit("new game response", { "error": False, "msg_code": MSG_CODE_PREPARE })
        else:
            emit("new game response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })

    # Check if NEW_GAME request while the server is in the STATE_READY or STATE_PLAY
    elif (state == STATE_READY or state == STATE_PLAY or state == STATE_END) and json["req"] == REQ_CODE_NEW_GAME:
        emit("new game response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })

    else:
        emit('new game response', { "error": True, "msg_code": MSG_CODE_BAD_REQ })


@socketio.on("client ready")
def client_ready_event(json):
    global state, \
           player_0, player_1, \
           players_ready

    # Validate input and return True if it is ok
    def __validate(input):
        if not "who" in input or not isinstance(input["who"], str):
            return False
        return True


    print("=========================")
    print("received client ready event: " + str(json))

    # Check if input is valid
    if not __validate(json):
        emit("client ready response", { "error": True, "msg_code": MSG_CODE_BAD_REQ })
        return

    # Check if CLIENT READY request while the server is in the STATE_READY
    if state == STATE_READY:# and json["req"] == REQ_CODE_CLIENT_READY:
        if json["who"] == player_0.getId() or json["who"] == player_1.getId():
            # Collect client ready messages before starting the game thread
            players_ready[json["who"]] = True

            print("Client ready: ", request.sid)
            print("Players ready: ", players_ready)

            if len(players_ready) == NUMBER_PLAYERS_GAME:
                # Go to next server state
                state = STATE_PLAY

                # Start game thread (blocking)
                #thread = socketio.start_background_task(target=game_thread)

                # Start non-blocking game thread
                eventlet.spawn_n(game_thread)

                # Start non-blocking game thread with a delay of X seconds
                #eventlet.spawn_after(2, game_thread)
        else:
            emit("client ready response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })

    # Check if player_0 quit while the server is in the STATE_READY and player_1 did not receive
    # the message emitted on event disconnect of player_0
    elif not player_0:
        emit("client ready response", { "error": False, "msg_code": MSG_CODE_GAME_END })

    # Check if player_1 quit while the server is in the STATE_READY and player_o did not receive
    # the message emitted on event disconnect of player_1
    elif not player_1:
        emit("client ready response", { "error": False, "msg_code": MSG_CODE_GAME_END })

    else:
        emit("client ready response", { "error": True, "msg_code": MSG_CODE_BAD_REQ })


@socketio.on("new move request")
def new_move_event(json):
    # Validate input and return True if it is ok
    def __validate(input):
        if not "who" in input or not isinstance(input["who"], str):
            return False
        if not "req" in input or not isinstance(input["req"], int):
            return False
        if not "value" in input or not isinstance(input["value"], float):
            return False
        return True

    print("=========================")
    print("received new move event: " + str(json))

    # Check if input is valid
    if not __validate(json):
        emit("new move response", { "error": True, "msg_code": MSG_CODE_BAD_REQ })
        return

    print("Player 0 id: ", player_0.getId())
    print("Player 1 id: ", player_1.getId())

    if state == STATE_PLAY and json["req"] == REQ_CODE_NEW_MOVE:
        if json["who"] == player_0.getId():
            # Use the received value to change the y component of player_0 speed
            player_0.setSpeed(json["value"])
            #emit("new move response", { "error": False, "msg_code": "NEW MOVE ACCEPTED" })
        elif json["who"] == player_1.getId():
            # Use the received value to change the y component of player_1 speed
            player_1.setSpeed(json["value"])
            #emit("new move response", { "error": False, "msg_code": "NEW MOVE ACCEPTED" })
    else:
        emit("new move response", { "error": True, "msg_code": MSG_CODE_BAD_REQ })


def __pickBuilding():
    building = random.choice(buildings)

    # Check if cpu_building variable has been initialized
    if cpu_building:
        # It has been init, try to pick a different cpu_building than the current one
        for i in range(3):
            if building.getId() != cpu_building.getId():
                break

            building = random.choice(buildings)

    # Height ratio normalized between [0,1] = Screen percentage
    #height_ratio = random.randrange(20, 58) / 100.0

    # Building pos Y in meters
    building_pos_y = random.randrange(17, 48)

    #building.setX(15.0)
    #building.setY(height_ratio)
    building.setY(building_pos_y)

    # Update speed
    building.setSpeed(speed)

    return building

def __pickVehicle():
    vehicle = random.choice(vehicles)

    # Check if cpu_vehicle variable has been initialized
    if cpu_vehicle:
        # It has been init, try to pick a different cpu_vehicle than the current one
        for i in range(3):
            if vehicle.getId() != cpu_vehicle.getId():
                break

            vehicle = random.choice(vehicles)

    # Height ratio normalized between [0,1] = Screen percentage
    #height_ratio = random.randrange(0, 20) / 100.0

    # Vehicle pos Y in meters
    #vehicle_pos_y = random.randrange(0, 17)
    vehicle_pos_y = random.randrange(0, 5)

    #vehicle.setX(15.0)
    #vehicle.setY(height_ratio)
    vehicle.setY(vehicle_pos_y)

    # Update speed (cpu_vehicle speed different from cpu_building one for more realism)
    vehicle.setSpeed(speed + 5.0)

    return vehicle

# Check if the players experiences a collision.
# Return a boolean tuple, true if a collision happened
def __checkCollisions():
    #return (False, False) # TEMP DISABLE CHECK OF COLLISIONS

    def createRect(x, y, w, h):
        # Sum values here because it cannot be done inside dict definition
        x2 = x+w
        y2 = y+h
        return { "x1": x, "y1": y, "x2": x2, "y2": y2 }

    # Create rects based on PPM of the player
    def createRects(obstacles, player):
        obstacles_rects = []

        for obstacle in obstacles:
            obstacle_rect = createRect(obstacle.getX(), # In meters
                                       obstacle.getY(), # In height ratio
                                       obstacle.getWidth(), # In meters
                                       obstacle.getHeight() * player.getPPM() / player.getScreenHeight() # In height ratio
            )

            obstacles_rects.append(obstacle_rect)

        return obstacles_rects

    def checkCollisionsAux(list_rects_a, list_rects_b):
        for r1 in list_rects_a:
            for r2 in list_rects_b:
                if intersect(r1, r2):
                    return True
        return False

    def intersect(rect_a, rect_b):
        if not rect_a or not rect_b:
            return False

        '''print("rect_a[\"x2\"]: ", rect_a["x2"])
        print("rect_b[\"x1\"]: ", rect_b["x1"])
        print("rect_a[\"x1\"]: ", rect_a["x1"])
        print("rect_b[\"x2\"]: ", rect_b["x2"])'''

        if rect_a["x2"] >= rect_b["x1"] and rect_a["x1"] <= rect_b["x2"] and \
           rect_a["y2"] >= rect_b["y1"] and rect_a["y1"] <= rect_b["y2"]:
           return True
        return False

    # X verifications in meters while Y in height ratios
    '''player_0_rect = createRect(player_0.getX(), player_0.getY(), player_0.getBitmapWidth(), player_0.getBitmapHeightScale())
    player_1_rect = createRect(player_1.getX(), player_1.getY(), player_1.getBitmapWidth(), player_1.getBitmapHeightScale())

    obstacles = [cpu_building, cpu_vehicle]
    # Create rects based on PPM of each player
    obstacles_0_rects = createRects(obstacles, player_0)
    obstacles_1_rects = createRects(obstacles, player_1)

    res_0 = checkCollisionsAux(player_0_rect, obstacles_0_rects)
    res_1 = checkCollisionsAux(player_1_rect, obstacles_1_rects)'''


    # All verifications in meters (VERSION WITH ONE RECT FOR EACH OBJECT)
    '''player_0_rect = createRect(player_0.getX(), player_0.getY(), player_0.getBitmapWidth(), player_0.getBitmapHeight())
    player_1_rect = createRect(player_1.getX(), player_1.getY(), player_1.getBitmapWidth(), player_1.getBitmapHeight())

    cpu_building_rect = createRect(
        cpu_building.getX(),
        cpu_building.getY(),
        cpu_building.getWidth(),
        cpu_building.getHeight())
    cpu_vehicle_rect  = createRect(
        cpu_vehicle.getX(),
        cpu_vehicle.getY(),
        cpu_vehicle.getWidth(),
        cpu_vehicle.getHeight())

    obstacles_rects = [cpu_building_rect, cpu_vehicle_rect]
    res_0 = checkCollisionsAux(player_0_rect, obstacles_rects)
    res_1 = checkCollisionsAux(player_1_rect, obstacles_rects)'''


    # All verifications in meters (VERSION WITH MULTIPLE RECTS FOR EACH OBJECT)
    player_0_rects = player_0.getBounds()
    player_1_rects = player_1.getBounds()

    #cpu_building_rects = cpu_building.getBounds()
    #cpu_vehicle_rects  = cpu_vehicle.getBounds()
    obstacles_rects = cpu_building.getBounds() + cpu_vehicle.getBounds()

    res_0 = checkCollisionsAux(player_0_rects, obstacles_rects)
    res_1 = checkCollisionsAux(player_1_rects, obstacles_rects)

    return (res_0, res_1)

# Thread that updates game logic
def game_thread():
    global state, \
           player_0, player_1, \
           cpu_building, cpu_vehicle, \
           score, speed, winner

    fps = 90
    target_frame_time   = 1.0 / fps  # In s
    time_previous_frame = 0          # In s

    print("Player 0 id: ", player_0.getId())
    print("Player 1 id: ", player_1.getId())

    print("Start game thread")
    while state == STATE_PLAY and winner == UNDEFINED:
        # Compute time difference
        #time_current_frame = int(round(time.time() * 1000))                           # In ms
        time_current_frame = time.time()                                       # In s
        dt = min(time_current_frame - time_previous_frame, target_frame_time)  # In s
        #dt = target_frame_time

        time_previous_frame = time_current_frame

        #print("time_current_frame: ", time_current_frame)
        #print("time_previous_frame: ", time_previous_frame)
        #print("dt: ", dt, " s")

        # Update players positions
        player_0.update(dt)
        player_1.update(dt)

        # Update cpu_building position
        cpu_building.update(dt)

        # Check if the cpu_building has been respawn
        if cpu_building.isRespawn():
            cpu_building = __pickBuilding()
            #print("new cpu_building ID: " + cpu_building.getId())

        # Update cpu_vehicle position
        cpu_vehicle.update(dt)

        # Check if the cpu_vehicle has been respawn
        if cpu_vehicle.isRespawn():
            cpu_vehicle = __pickVehicle()
            #print("new cpu_vehicle ID: " + cpu_vehicle.getId())


        #print("cpu_building X: ", cpu_building.getX())
        #print("cpu_vehicle X: ", cpu_vehicle.getX())


        # Check players collisions
        is_player_0_collided, is_player_1_collided = __checkCollisions()

        # Check if there is a winner in this frame
        if is_player_0_collided and is_player_1_collided:
            winner   = WINNER_DRAW
            state    = STATE_END
            msg_code = MSG_CODE_GAME_END
        elif is_player_0_collided:
            winner   = WINNER_PLAYER_1
            state    = STATE_END
            msg_code = MSG_CODE_GAME_END
        elif is_player_1_collided:
            winner   = WINNER_PLAYER_0
            state    = STATE_END
            msg_code = MSG_CODE_GAME_END
        else:
            # Update the score
            score += speed * dt

            # Update the speed (bounded by 40 m/s)
            if speed < 40.0:
                speed += 0.001

            msg_code = MSG_CODE_GAME_PLAY

            #Compute time to wait to be consistent with the target frame time
            #time_current_frame_end = int(round(time.time() * 1000))                           # In ms
            time_current_frame_end = time.time()                                           # In s
            wait_time = target_frame_time - (time_current_frame_end - time_current_frame)  # In s
            #wait_time = target_frame_time

            #print("wait_time: ", wait_time)

            if wait_time > 0:
                time.sleep(wait_time)

        with app.app_context():
            socketio.emit("game update",
                 { "error": False,
                   "msg_code": msg_code,
                   "cpu_building": { "id": cpu_building.getId(),
                                     "pos_x": cpu_building.getX(),
                                     "pos_y": cpu_building.getY(),
                                     #"bounds": cpu_building.getBounds()#,
                                     #"width": cpu_building.getWidth(),
                                     #"height": cpu_building.getHeight()
                                   },
                   "cpu_vehicle": { "id": cpu_vehicle.getId(),
                                    "pos_x": cpu_vehicle.getX(),
                                    "pos_y": cpu_vehicle.getY(),
                                    #"bounds": cpu_vehicle.getBounds()#,
                                    #"width": cpu_vehicle.getWidth(),
                                    #"height": cpu_vehicle.getHeight()
                                  },
                   "player_0": { "id": player_0.getId(),
                                 "pos_x": player_0.getX(),
                                 "pos_y": player_0.getY(),
                                 "rotation": player_0.getRotation()#,
                                 #"width": player_0.getBitmapWidth(),
                                 #"height": player_0.getBitmapHeight()'''
                               },
                   "player_1": { "id": player_1.getId(),
                                 "pos_x": player_1.getX(),
                                 "pos_y": player_1.getY(),
                                 "rotation": player_1.getRotation()#,
                                 #"width": player_1.getBitmapWidth(),
                                 #"height": player_1.getBitmapHeight()
                               },
                   "score": score,
                   "winner": winner
                 }, to=room )

            #print("Game update message sent to clients in room: ", room)

    print("Game end! score: ", score, " winner: ", winner)


if __name__ == "__main__":
    socketio.run(app, host="0.0.0.0", debug=True)
    #socketio.run(app)
