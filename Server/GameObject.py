class GameObject:
    def __init__(self, id, width, height, pos_x, pos_y, speed):
        self.id = id

        '''self.bounds_offsets = bounds_offsets
        self.bounds = []
        for offset_array in bounds_offsets:
            bounds.append( RectF() )'''

        self.width  = width   # In meters
        self.height = height  # In meters

        self.pos_x = pos_x  # In meters
        self.pos_y = pos_y  # In meters

        self.speed = speed  # In m/s
        #self.speed = speed  # In %/s

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
