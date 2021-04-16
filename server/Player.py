class Player:
    def __init__(self, id, screen_width, screen_height, world_width, bitmap_width, bitmap_height, pos_x, pos_y):
        self.id = id

        self.bitmap_width  = bitmap_width  # In meters
        self.bitmap_height = bitmap_height # In meters
        
        self.pos_x = pos_x, # In meters
        self.pos_x = pos_y  # In height ratio

        self.screen_width  = screen_width  # In pixels
        self.screen_height = screen_height # In pixels
        self.ppm = Math.min(screen_width / world_width, screen_height / world_width)

        self.height_scale = height * ppm / screen_height # In height ratio (5/10 = 0.5)
        #self.height_scale = screen_height / height * ppm # In height ratio (10/5 = 2)

    # Getters
    def getId(self):
        return self.id

    def getScreenWidth(self):
        return self.screen_width

    def getScreenHeight(self):
        return self.screen_height

    def getWidth(self):
        return self.width

    def getHeight(self):
        return self.height

    def getHeightScale(self):
        return self.height_scale

    def getX(self):
        return self.pos.x

    def getY(self):
        return self.pos.y

    def getSpeed(self):
        return self.speed

    # Setters
    def setX(self, pos_x):
        self.pos.x = pos_x

    def setY(self, pos_y):
        self.pos.y = pos_y
