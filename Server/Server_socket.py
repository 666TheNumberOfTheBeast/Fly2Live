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
STATE_PLAY    = 3
STATE_END     = 4

# Constants for the players IDs
UNDEFINED  = -1
DRAW       = 2

# Constants for the client requests
REQ_CODE_NEW_GAME = 0
REQ_CODE_NEW_MOVE = 1

# Constants for the messages
MSG_CODE_BAD_REQ       = 0
MSG_CODE_SEARCHING_ADV = 1
MSG_CODE_FOUND_ADV     = 2
MSG_CODE_PREPARE       = 3
MSG_CODE_GAME_START    = 4
MSG_CODE_GAMEPLAY      = 5
MSG_CODE_GAME_END      = 6
MSG_CODE_SERVER_BUSY   = 7

# Variables of the server for keeping the game state
state = STATE_INIT

# Dictionary of players currently connected to the server
playersConnected = {}
playersInRoom    = []

# For using multiple rooms, I need multithread for properly handling below server variables
#rooms   = []
room = "game"

player_0 = None
player_1 = None
cpu_building = None
cpu_vehicle  = None

world_width  = 50.0  # In meters
world_height = 60.0  # In meters
speed = 10.0         # In m/s

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

buildings = [
    Obstacle("building_01", 25.0, 70.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_02", 10.0, 80.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_03", 25.0, 110.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_04", 35.0, 90.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_05", 35.0, 50.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_06", 45.0, 50.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_07", 15.0, 80.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_08", 13.0, 70.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_09", 17.0, 70.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("building_10", 13.0, 60.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed)
]

obstacle_initial_pos_y = 20.0  # In meters
#obstacle_initial_pos_y = 0.05  # In height ratio

vehicles = [
    Obstacle("vehicle_01", 6.0, 2.5, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("vehicle_02", 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("vehicle_03", 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("vehicle_04", 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("vehicle_05", 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("vehicle_06", 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("vehicle_07", 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("vehicle_08", 20.0, 8.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed),
    Obstacle("vehicle_09", 4.0, 2.0, obstacle_initial_pos_x, obstacle_initial_pos_y, speed)
]


app = Flask(__name__)
app.config["SECRET_KEY"] = "my_secret_key"
#socketio = SocketIO(app)

# monkey patching is necessary because this application uses a background thread
socketio = SocketIO(app, async_mode="eventlet")
import eventlet
eventlet.monkey_patch()

# Prova al posto di start_background_task (non invia i messaggi)
from threading import Thread


# ********* APIs *********
# TODO: bind each user to a UID (with Google login is not necessary, use the Google one)
#@app.route('/', methods=['GET'])
#def home():
#    return "<h1>Distant Reading Archive</h1><p>This site is a prototype API for distant reading of science fiction novels.</p>"
# ************************


# Event connection to the server
@socketio.on("connect")
def connect():
    # No need global variables declaration to read but not modify them
    print("=========================")
    print("Client connected: " + request.sid)

    # Add the player socket ID to the dictionary of players currently connected to the server
    playersConnected[request.sid] = { "id": None, "room": None }

    print("Players connected to the server: ", playersConnected)

    # Check if a room is available (multithread version)
    '''if not rooms:
        room = request.sid
        rooms.append(room)
    else:
        room = rooms.pop(0)'''

    # Check if the room is available (monothread version)
    if len(playersInRoom) == 2:
        # Room is busy
        return

    print("Player added to the room: " + room)
    playersConnected[request.sid]["room"] = room

    playersInRoom.append(request.sid)

    # Add the player to an available room
    join_room(room, sid=request.sid)

# Event join a room
'''@socketio.on('join')
def on_join(data):
    username = data['username']
    room = data['room']
    join_room(room)
    send(username + ' has entered the room.', to=room)'''


@socketio.on("disconnect")
def disconnect():
    # Declare global variables that will be modified by this function
    global state, \
           player_0, player_1, \
           cpu_building, cpu_vehicle, \
           score, winner

    print("=========================")
    print("Client disconnected: " + request.sid)

    leave_room(room, sid=request.sid)

    if request.sid in playersInRoom:
        playersInRoom.remove(request.sid)

    player_id = playersConnected[request.sid]["id"]
    print("Player ID: ", player_id)
    print("Server state: ", state)

    # Check if player quit before sending NEW_GAME request
    if not player_id:
        # Remove the player socket ID from the dictionary of players connected to the server
        playersConnected.pop(request.sid)
        return

    # Check if player_0 quit while the server is in the STATE_SEARCH
    if state == STATE_SEARCH and player_id == player_0.getId():
        player_0 = None
        state = STATE_INIT
    # Check if player_1 quit while the server is in the STATE_SEARCH
    elif state == STATE_SEARCH and player_id == player_1.getId():
        player_1 = None
        state = STATE_INIT
    # Check if player_0 quit while the server is in the STATE_PREPARE
    elif state == STATE_PREPARE and player_id == player_0.getId():
        player_0 = None
        state = STATE_SEARCH
        emit("new game response",
            { "error": False,
              "msg_code": MSG_CODE_SEARCHING_ADV
            }, to=room)
    # Check if player_1 quit while the server is in the STATE_PREPARE
    elif state == STATE_PREPARE and player_id == player_1.getId():
        player_1 = None
        state = STATE_SEARCH
        emit("new game response",
            { "error": False,
              "msg_code": MSG_CODE_SEARCHING_ADV
            }, to=room)
    # Check if player_0 quit while the server is in the STATE_PLAY
    elif state == STATE_PLAY and player_id == player_0.getId():
        winner = player_1
        emit("game update",
            { "error": False,
              "msg_code": MSG_CODE_GAME_END,
              "cpu_building": { "id": cpu_building.getId(),
                                "pos_x": cpu_building.getX(),
                                "pos_y": cpu_building.getY(),
                                "width": cpu_building.getWidth(),
                                "height": cpu_building.getHeight()
                              },
              "cpu_vehicle": { "id": cpu_vehicle.getId(),
                               "pos_x": cpu_vehicle.getX(),
                               "pos_y": cpu_vehicle.getY(),
                               "width": cpu_vehicle.getWidth(),
                               "height": cpu_vehicle.getHeight()
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
        state = STATE_INIT
    # Check if player_1 quit while the server is in the STATE_PLAY
    elif state == STATE_PLAY and player_id == player_1.getId():
        winner = player_0
        emit("game update",
            { "error": False,
              "msg_code": MSG_CODE_GAME_END,
              "cpu_building": { "id": cpu_building.getId(),
                                "pos_x": cpu_building.getX(),
                                "pos_y": cpu_building.getY(),
                                "width": cpu_building.getWidth(),
                                "height": cpu_building.getHeight()
                              },
              "cpu_vehicle": { "id": cpu_vehicle.getId(),
                               "pos_x": cpu_vehicle.getX(),
                               "pos_y": cpu_vehicle.getY(),
                               "width": cpu_vehicle.getWidth(),
                               "height": cpu_vehicle.getHeight()
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
        state = STATE_INIT

    # Remove the player socket ID from the dictionary of players connected to the server
    playersConnected.pop(request.sid)


@socketio.on("new game request")
def new_game_event(json):
    global state, \
           player_0, player_1, \
           cpu_building, cpu_vehicle, \
           score, winner

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
        return True

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
        # Add player ID to the dictionary of players connected to the server
        playersConnected[request.sid]["id"] = json["who"]

        # Reset last players IDs variables
        player_0 = Player(
            json["who"], json["screen_width"], json["screen_height"], world_width,
            player_bitmap_width, player_bitmap_height,
            player_initial_pos_x, player_initial_pos_y, 0.0)
        player_1 = None

        # Reset obstacles variables
        cpu_building = None
        cpu_vehicle  = None

        # Reset winner
        winner = UNDEFINED

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
            if not player_0:
                player_0 = Player(
                    json["who"], json["screen_width"], json["screen_height"], world_width,
                    player_bitmap_width, player_bitmap_height,
                    player_initial_pos_x, player_initial_pos_y, 0.0)
            elif not player_1:
                player_1 = Player(
                    json["who"], json["screen_width"], json["screen_height"], world_width,
                    player_bitmap_width, player_bitmap_height,
                    player_initial_pos_x, player_initial_pos_y, 0.0)
            else:
                # Just for super-safety but this condition should never be true
                # because the server should be in STATE_PREPARE
                emit("new game response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })
                return

            # Go to next server state
            state = STATE_PREPARE

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
            state = STATE_PLAY

            emit("new game response",
                { "error": False,
                  "msg_code": MSG_CODE_GAME_START
                }, to=room )

            # Start game thread (blocking)
            #thread = socketio.start_background_task(target=game_thread)

            # Non-blocking spawning of the game thread
            eventlet.spawn_n(game_thread)

    # Check if NEW_GAME request while the server is in the STATE_PREPARE
    elif state == STATE_PREPARE and json["req"] == REQ_CODE_NEW_GAME:
        if json["who"] == player_0.getId() or json["who"] == player_1.getId():
            emit("new game response", { "error": False, "msg_code": MSG_CODE_PREPARE })
        else:
            emit("new game response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })

    # Check if NEW_GAME request while the server is in the STATE_PLAY
    elif state == STATE_PLAY and json["req"] == REQ_CODE_NEW_GAME:
        emit("new game response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })

    # Check if NEW_GAME request while the server is in the STATE_END
    #elif state == STATE_END and json["req"] == REQ_CODE_NEW_GAME:
    #    emit("new game response", { "error": False, "msg_code": MSG_CODE_SERVER_BUSY })

    else:
        emit('new game response', { "error": True, "msg_code": MSG_CODE_BAD_REQ })


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

    #building.setX(obstacle_initial_pos_x)
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
    vehicle_pos_y = random.randrange(0, 17)

    #vehicle.setX(obstacle_initial_pos_x)
    #vehicle.setY(height_ratio)
    vehicle.setY(vehicle_pos_y)

    # Update speed (cpu_vehicle speed different from cpu_building one for more realism)
    vehicle.setSpeed(speed + 5.0)

    return vehicle

# Check if the players experiences a collision.
# Return a boolean tuple, true if a collision happened
def __checkCollisions():
    return (False, False) # TEMP DISABLE CHECK OF COLLISIONS

    def createRect(x, y, w, h):
        # Sum values here because it cannot be done inside dict definition
        x2 = w+h
        y2 = y+h
        return { "x1": x, "y1": y, "x2": x2, "y2": y2 }

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

    def checkCollisionsAux(rect, list_rects):
        for r in list_rects:
            if intersect(rect, r):
                return True
        return False

    def intersect(rect_a, rect_b):
        '''print("rect_a[\"x2\"]: ", rect_a["x2"])
        print("rect_b[\"x1\"]: ", rect_b["x1"])
        print("rect_a[\"x1\"]: ", rect_a["x1"])
        print("rect_b[\"x2\"]: ", rect_b["x2"])'''
        if rect_a["x2"] >= rect_b["x1"] and rect_a["x1"] <= rect_b["x2"] and \
           rect_a["y2"] >= rect_b["y1"] and rect_a["y1"] <= rect_b["y2"]:
           return True
        return False

    # X verifications in meters while Y in height ratios
    player_0_rect = createRect(player_0.getX(), player_0.getY(), player_0.getBitmapWidth(), player_0.getBitmapHeightScale())
    player_1_rect = createRect(player_1.getX(), player_1.getY(), player_1.getBitmapWidth(), player_1.getBitmapHeightScale())

    obstacles = [cpu_building, cpu_vehicle]
    obstacles_0_rects = createRects(obstacles, player_0)
    obstacles_1_rects = createRects(obstacles, player_1)

    res_0 = checkCollisionsAux(player_0_rect, obstacles_0_rects)
    res_1 = checkCollisionsAux(player_1_rect, obstacles_1_rects)

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

    # Wait a bit before starting the game loop
    time.sleep(2.5)

    print("Start game thread")
    while state == STATE_PLAY and winner == UNDEFINED:
        # Compute time difference
        time_current_frame = int(round(time.time()))                           # In s
        dt = min(time_current_frame - time_previous_frame, target_frame_time)  # In s
        #dt = target_frame_time

        time_previous_frame = time_current_frame

        print("dt: ", dt, " s")

        # Update players positions
        player_0.update(dt)
        player_1.update(dt)

        # Update cpu_building position
        cpu_building.update(dt)

        # Check if the cpu_building has been respawn
        if cpu_building.isRespawn():
            cpu_building = __pickBuilding()
            print("new cpu_building ID: " + cpu_building.getId())

        # Update cpu_vehicle position
        cpu_vehicle.update(dt)

        # Check if the cpu_vehicle has been respawn
        if cpu_vehicle.isRespawn():
            cpu_vehicle = __pickVehicle()
            print("new cpu_vehicle ID: " + cpu_vehicle.getId())


        print("cpu_building X: ", cpu_building.getX())
        print("cpu_vehicle X: ", cpu_vehicle.getX())


        # Check players collisions
        is_player_0_collided, is_player_1_collided = __checkCollisions()

        # Check if there is a winner in this frame
        if is_player_0_collided and is_player_1_collided:
            winner   = DRAW
            state    = STATE_END
            msg_code = MSG_CODE_GAME_END
            # TODO: Select two players from the dictionary for a new game and repeat steps in new game request event
        elif is_player_0_collided:
            winner   = player_1
            state    = STATE_END
            msg_code = MSG_CODE_GAME_END
        elif is_player_1_collided:
            winner   = player_0
            state    = STATE_END
            msg_code = MSG_CODE_GAME_END
        else:
            # Update the score
            score += speed * dt

            # Update the speed (bounded by 40 m/s)
            if speed < 40.0:
                speed += 0.001

            msg_code = MSG_CODE_GAMEPLAY

            #Compute time to wait to be consistent with the target frame time
            time_current_frame_end = int(round(time.time()))                               # In s
            wait_time = target_frame_time - (time_current_frame_end - time_current_frame)  # In s
            #wait_time = target_frame_time

            print("wait_time: ", wait_time)

            if wait_time > 0:
                time.sleep(wait_time)

        with app.app_context():
            socketio.emit("game update",
                 { "error": False,
                   "msg_code": msg_code,
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
                 }, to=room )

            print("Game update message sent to clients in room: ", room)


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

    print("received new move event: " + str(json))

    # Check if input is valid
    if not __validate(json):
        emit("new move response", { "error": True, "message": "INVALID REQUEST" })
        return

    if state == STATE_PLAY and json["req"] == REQ_CODE_NEW_MOVE:
        if json["who"] == player_0.getId():
            # Use the received value to change the y component of player_0 speed
            player_0.setSpeed(json["value"])
            emit("new move response", { "error": False, "message": "NEW MOVE ACCEPTED" })
        elif json["who"] == player_1.getId():
            # Use the received value to change the y component of player_1 speed
            player_1.setSpeed(json["value"])
            emit("new move response", { "error": False, "message": "NEW MOVE ACCEPTED" })
    else:
        emit("new move response", { "error": True, "message": "INVALID REQUEST" })


if __name__ == "__main__":
    socketio.run(app, host="0.0.0.0", debug=True)
    #socketio.run(app)
