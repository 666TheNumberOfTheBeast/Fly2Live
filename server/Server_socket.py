# ********* ABOUT *********
# Server for handling a single game at time (2 players play, the other wait the server resources are free)
# *************** *********

from flask         import Flask
from flask_restful import Api, Resource, reqparse

from flask_socketio import SocketIO, send, emit

#from pygame import Rect

from Player   import *
from Obstacle import *

import random


# Constants for the server state
STATE_INIT    = 0
STATE_WAIT    = 1
STATE_PREPARE = 2
STATE_PLAY    = 3
STATE_END     = 4

# Constants for the client requests
NEW_GAME = 0
POLLING  = 1
NEW_MOVE = 2

# Constants for the players IDs
UNDEFINED  = -1
#PLAYER_0   = 0
#PLAYER_1   = 1
DRAW = 2

# Variables of the server for keeping the game state
state = STATE_INIT

player_0 = None
player_1 = None
building = None
vehicle  = None

world_width = 30.0 # In meters
# Versione senza dt in update
#speed = 0.2        # In m/s
# Versione con dt in update
speed = 10.0        # In m/s

winner = UNDEFINED


# ********* APIs *********
# TODO: bind each user to a UID (with Google login maybe this is not necessary)
#@app.route('/', methods=['GET'])
#def home():
#    return "<h1>Distant Reading Archive</h1><p>This site is a prototype API for distant reading of science fiction novels.</p>"
# ************************


respawn_pos_x = world_width + 3.0

player_bitmap_width  = 8.0 # In meters
player_bitmap_height = 3.0 # In meters
player_initial_pos_x = 1.0 # In meters
player_initial_pos_y = 0.5 # In height ratio

now      = 0 # In ms
previous = 0 # In ms
score = 0.0

# Height ratios normalized in [0,1]
# NB: y goes from 0 to height = from top to bottom
# 10/5 = 2 => 5/10 = 0.5

buildings = [
    Obstacle("building_01", 15.0, 100.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_02", 10.0, 90.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_03", 25.0, 110.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_04", 30.0, 90.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_05", 35.0, 60.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_06", 35.0, 40.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_07", 15.0, 80.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_08", 10.0, 70.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_09", 17.0, 70.0, respawn_pos_x, 0.5, speed, respawn_pos_x),
    Obstacle("building_10", 10.0, 65.0, respawn_pos_x, 0.5, speed, respawn_pos_x)
]

vehicles = [
    Obstacle("biplane", 7.0, 3.0, respawn_pos_x, 0.05, speed, respawn_pos_x),
    Obstacle("dirigible_1", 20.0, 8.0, respawn_pos_x, 0.05, speed, respawn_pos_x),
    Obstacle("dirigible_2", 20.0, 8.0, respawn_pos_x, 0.05, speed, respawn_pos_x),
    Obstacle("dirigible_3", 20.0, 8.0, respawn_pos_x, 0.05, speed, respawn_pos_x),
    Obstacle("dirigible_4", 20.0, 8.0, respawn_pos_x, 0.05, speed, respawn_pos_x),
    Obstacle("dirigible_5", 20.0, 8.0, respawn_pos_x, 0.05, speed, respawn_pos_x),
    Obstacle("dirigible_6", 20.0, 8.0, respawn_pos_x, 0.05, speed, respawn_pos_x),
    Obstacle("dirigible_7", 20.0, 8.0, respawn_pos_x, 0.05, speed, respawn_pos_x),
    Obstacle("ufo", 4.0, 2.0, respawn_pos_x, 0.05, speed, respawn_pos_x)
]


app = Flask(__name__)
app.config["SECRET_KEY"] = "my_secret_key"
socketio = SocketIO(app)


def __pickBuilding():
    building = random.choice(buildings)

    # Non normalized, used in height/height_ratio
    #height_ratio = random.randrange(13, 50) / 10.0

    # normalized between [0,1]
    height_ratio = random.randrange(20, 75) / 100.0

    building.setX(world_width + 3.0)
    building.setY(height_ratio)

def __pickVehicle():
    vehicle = random.choice(vehicles)

    # Non normalized, used in height/height_ratio
    #height_ratio = random.randrange(80, 100) / 10.0

    # normalized between [0,1]
    height_ratio = random.randrange(0, 20) / 100.0

    building.setX(world_width + 3.0)
    building.setY(height_ratio)

# Check if the players experiences a collision.
# Return a boolean tuple, true if a collision happened
def __checkCollisions():
    def createRect(x, y, w, h):
        return { "x1": x, "y1": y, "x2": x+w, "y2": y+h }

    def createRects(obstacles, player):
        obstacles_rects = []

        for obstacle in obstacles:
            obstacles_rect = createRect(obstacle.getX(), # In meters
                                        obstacle.getY(), # In height ratio
                                        obstacle.getWidth(), # In meters
                                        obstacle.getHeight() * player.getPPM() / player.getScreenHeight() # In height ratio
            )

            obstacle_rects.append(obstacle_rect)

        return obstacle_rects

    def checkCollisionsAux(rect, list_rects):
        for r in list_rects:
            if intersect(rect, r):
                return True
        return False

    def intersect(rect_a, rect_b):
        if rect_a.x2 >= rect_b.x1 and rect_a.x1 <= rect_b.x2 and \
           rect_a.y2 >= rect_b.y1 and rect_a.y1 <= rect_b.y2:
           return True
        return False

    # le X le verifico in metri mentre le Y con i rapporti delle altezze
    player_0_rect = createRect(player_0.getX(), player_0.getY(), player_0.getWidth(), player_0.getBitmapHeightScale())
    player_1_rect = createRect(player_1.getX(), player_1.getY(), player_1.getWidth(), player_1.getBitmapHeightScale())

    obstacles = [building, vehicle]
    obstacles_0_rects = createRects(obstacles, player_0)
    obstacles_1_rects = createRects(obstacles, player_1)

    res_0 = checkCollisionsAux(player_0_rect, obstacles_0_rects)
    res_1 = checkCollisionsAux(player_1_rect, obstacles_1_rects)

    return (res_0, res_1)

# Thread that updates game logic
def game_thread():
    # Constrain the bitmap in the screen
    def checkPlayerPosition(pos_y, bitmap_height_scale):
        if pos_y < 0:
            return 0.0
        elif pos_y + bitmap_height_scale > 1.0:
            return 1.0 - bitmap_height_scale
        else:
            return pos_y

    while state == STATE_PLAY and winner == UNDEFINED:
        # Compute time difference
        now = System.currentTimeMillis()
        dt = 0.0 # In s

        # Check if this is the first first frame to draw
        if previous != 0:
            dt = (now - previous) / 1000.0

        previous = now

        # Update players positions
        player_0_y = player_0.getY() + player_0.getSpeedY()
        player_1_y = player_1.getY() + player_1.getSpeedY()

        player_0_y = checkPlayerPosition(player_0_y, player_0.getBitmapHeightScale())
        player_1_y = checkPlayerPosition(player_1_y, player_1.getBitmapHeightScale())

        player_0.setY(player_0_y)
        player_1.setY(player_1_y)

        # Update obstacles positions
        building.update(dt)

        # Check if the building has been respawn
        if building.isRespawn():
            __pickBuilding()

        vehicle.update(dt)

        # Check if the vehicle has been respawn
        if vehicle.isRespawn():
            __pickVehicle()

        # Check players collisions
        player_0_is_collided, player_1_is_collided = __checkCollisions()

        if player_0_is_collided and player_1_is_collided:
            winner = DRAW
            state  = STATE_END
        elif player_0_is_collided:
            winner = player_1
            state  = STATE_END
        elif player_1_is_collided:
            winner = player_0
            state  = STATE_END

        # Since the score is updated to slowly using m/s,
        # I speeded it up
        #score += speed * 6 * dt
        score += speed * dt

        emit("new game response",
             { "error": False,
               "message": "GAME STARTED",
               "building": { "id": building.getId(),
                             "pos_x": building.getX(),
                             "pos_y": building.getY(),
                             "width": building.getWidth(),
                             "height": building.getHeight()
                           },
               "vehicle": { "id": vehicle.getId(),
                            "pos_x": vehicle.getX(),
                            "pos_y": vehicle.getY(),
                            "width": vehicle.getWidth(),
                            "height": vehicle.getHeight()
                          },
               "player_0": { "id": player_0.getId(),
                             "pos_x": player_0.getX(),
                             "pos_y": player_0.getY(),
                             "width": player_0.getWidth(),
                             "height": player_0.getHeight()
                           },
               "player_1": { "id": player_1.getId(),
                             "pos_x": player_1.getX(),
                             "pos_y": player_1.getY(),
                             "width": player_1.getWidth(),
                             "height": player_1.getHeight()
                           },
               "score": score,
               "winner": winner
             },
             broadcast=True )
        #time.sleep(5)

@socketio.on("connect")
def connect():
    print("A user connected")

@socketio.on("disconnect")
def disconnect():
    print("A user disconnected")


@socketio.on("new game request")
def new_game_event(json):
    global state, \
           player_0, player_1, \
           building, vehicle, \
           winner

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

    print("received new game event: " + str(json))

    # Check if input is valid
    if not __validate(json):
        emit("new game response", { "error": True, "message": "INVALID REQUEST" })
        return

    # Check if a NEW_GAME request arrives while the server is in the STATE_INIT
    if state == STATE_INIT and json["req"] == NEW_GAME:
        # Reset last players IDs variables
        player_0 = Player(json["who"], json["screen_width"], json["screen_height"], world_width,
                          player_bitmap_width, player_bitmap_height, player_initial_pos_x, player_initial_pos_y)
        player_1 = None

        # Reset obstacles variables
        building = None
        vehicle  = None

        # Reset winner
        winner = UNDEFINED

        state = STATE_WAIT
        emit("new game response", { "error": False, "message": "FINDING AN OPPONENT" })

    # Check if NEW_GAME request while the server is in the STATE_WAIT
    elif state == STATE_WAIT and json["req"] == NEW_GAME:
        # Check if NEW_GAME request from a different user than before
        if json["who"] != player_0.getId():
            player_1 = Player(json["who"], json["screen_width"], json["screen_height"], world_width,
                              player_bitmap_width, player_bitmap_height, player_initial_pos_x, player_initial_pos_y)

            state = STATE_PREPARE
            emit("new game response", { "error": False, "message": "OPPONENT FOUND" }, broadcast=True)

            __pickBuilding()
            __pickVehicle()

            state = STATE_PLAY

            # Start game thread
            thread = socketio.start_background_task(target=game_thread)

            emit("new game response",
                 { "error": False,
                   "message": "GAME STARTED",
                   "building": { "id": building.getId(),
                                 "pos_x": building.getX(),
                                 "pos_y": building.getY(),
                                 "width": building.getWidth(),
                                 "height": building.getHeight()
                               },
                   "vehicle": { "id": vehicle.getId(),
                                "pos_x": vehicle.getX(),
                                "pos_y": vehicle.getY(),
                                "width": vehicle.getWidth(),
                                "height": vehicle.getHeight()
                              },
                   "player_0": { "id": player_0.getId(),
                                 "pos_x": player_0.getX(),
                                 "pos_y": player_0.getY(),
                                 "width": player_0.getWidth(),
                                 "height": player_0.getHeight()
                               },
                   "player_1": { "id": player_1.getId(),
                                 "pos_x": player_1.getX(),
                                 "pos_y": player_1.getY(),
                                 "width": player_1.getWidth(),
                                 "height": player_1.getHeight()
                               },
                   "score": score,
                   "winner": winner
                 },
                 broadcast=True )
        else:
            emit("new game response", { "error": False, "message": "FINDING AN OPPONENT" })

    # Check if NEW_GAME request while the server is in the STATE_PREPARE
    elif state == STATE_PREPARE and json["req"] == NEW_GAME:
        if json["who"] == player_0.getId() or json["who"] == player_1.getId():
            emit("new game response", { "error": False, "message": "PREPARING THE GAME" })
        else:
            emit("new game response", { "error": False, "message": "SERVER BUSY, WAIT" })

    # Check if NEW_GAME request while the server is in the STATE_PLAY
    elif state == STATE_PLAY and json["req"] == NEW_GAME:
        emit("new game response", { "error": False, "message": "SERVER BUSY, WAIT" })

    else:
        emit('new game response', { "error": True, "message": "INVALID REQUEST" })



'''@socketio.on("new move request")
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

    if state == STATE_PLAY and json["req"] == NEW_MOVE:
        if json["who"] == player_0.getId():
            # Use the received value to change the y component of player_0 speed
            player_0.setSpeedY(json.value)
            emit("new move response", { "error": False, "message": "NEW MOVE ACCEPTED" })
        elif json["who"] == player_1.getId():
            # Use the received value to change the y component of player_1 speed
            player_1.setSpeedY(json.value)
            emit("new move response", { "error": False, "message": "NEW MOVE ACCEPTED" })
    else:
        emit("new move response", { "error": True, "message": "INVALID REQUEST" })'''


if __name__ == "__main__":
    socketio.run(app, host="0.0.0.0", debug=True)
    #socketio.run(app)
