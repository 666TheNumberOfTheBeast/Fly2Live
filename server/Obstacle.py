class Obstacle:
    def __init__(self, id, width, height, pos_x, pos_y, speed, respawn_pos_x):
        self.id = id

        self.width  = width  # In meters
        self.height = height # In meters

        self.pos_x = pos_x, # In meters
        self.pos_y = pos_y  # In height ratio

        self.speed = speed  # In m/s

        self.respawn = False
        self.respawn_pos_x = respawn_pos_x

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
        self.pos_x = pos_x

    def setY(self, pos_y):
        self.pos_y = pos_y

    def setSpeed(self, speed):
        self.speed = speed

    # Update UI
    def update(self, dt):
        #self.pos_x -= self.speed
        self.pos_x -= self.speed * dt

        if (self.pos_x + self.width < 0):
            self.pos_x = self.respawn_pos_x
            self.respawn = True
        else:
            self.respawn = False

    # Return true if the vehicle has been respawn in the current UI update
    def isRespawn(self):
        return self.respawn
