from GameObject import *

class Obstacle(GameObject):
    def __init__(self, id, bounds_offsets, width, height, pos_x, pos_y, speed):
        super().__init__(id, bounds_offsets, width, height, pos_x, pos_y, speed)

        self.respawn = False
        self.respawn_pos_x = pos_x  # In meters
        #self.respawn_pos_x = 1.02 # In width ratio

    # Update UI
    def update(self, dt):
        #super().update(dt)

        # Move object to left
        self.setX( self.getX() - self.getSpeed() * dt )

        # Check if it is outside the left margin of the screen
        if self.getX() + self.getWidth() < 0.0:
            self.setX(self.respawn_pos_x)
            self.respawn = True
        else:
            self.respawn = False

        super().update(dt)

    # Return true if the vehicle has been respawn in the current UI update
    def isRespawn(self):
        return self.respawn

















'''class Obstacle:
    def __init__(self, id, width, height, pos_x, pos_y, speed):
        self.id = id

        self.width  = width   # In meters
        self.height = height  # In meters

        self.pos_x = pos_x  # In width ratio
        self.pos_y = pos_y  # In height ratio

        self.speed = speed  # In m/s
        #self.speed = speed  # In %/s

        self.respawn = False
        self.respawn_pos_x = obstacle_initial_pos_x
        #self.respawn_pos_x = 1.02 # In width ratio

    # Getters
    def getId(self):
        return self.id

    def getWidth(self):
        return self.width

    def getHeight(self):
        return self.height

    def getX(self):
        return self.pos_x

    def getY(self):
        return self.pos_y

    def getSpeed(self):
        return self.speed

    # Setters
    def setX(self, pos_x):
        self.pos_x = pos_x  # In meters

    def setY(self, pos_y):
        self.pos_y = pos_y  # In meters

    def setSpeed(self, speed):
        self.speed = speed  # In m/s
        #self.speed = speed  # In %/s

    # Update UI
    def update(self, dt):
        self.pos_x -= self.speed * dt

        if (self.pos_x + self.width < 0.0):
            self.pos_x = self.respawn_pos_x
            self.respawn = True
        else:
            self.respawn = False

    # Return true if the vehicle has been respawn in the current UI update
    def isRespawn(self):
        return self.respawn'''
