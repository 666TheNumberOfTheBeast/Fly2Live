class GameObject:
    def __init__(self, id, bounds_offsets, width, height, pos_x, pos_y, speed):
        self.id = id

        self.bounds_offsets = bounds_offsets
        self.bounds = []
        for offset_array in bounds_offsets:
            self.bounds.append({})

        self.width  = width   # In meters
        self.height = height  # In meters

        self.pos_x = pos_x  # In meters
        self.pos_y = pos_y  # In meters

        self.speed = speed  # In m/s
        #self.speed = speed  # In %/s

    # Getters
    def getId(self):
        return self.id

    def getBounds(self):
        return self.bounds.copy()

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
        # Set physics bounds
        for i in range(len(self.bounds_offsets)):
            offset_array = self.bounds_offsets[i]

            if len(offset_array) == 4:
                b = self.bounds[i]
                b["x1"] = self.getX() + offset_array[0] * self.getWidth()
                b["y1"] = self.getY() + offset_array[1] * self.getHeight()
                b["x2"] = self.getX() + offset_array[2] * self.getWidth()
                b["y2"] = self.getY() + offset_array[3] * self.getHeight()
