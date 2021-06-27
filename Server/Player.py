from GameObject import *

class Player:
    def __init__(self, player_id, screen_width, screen_height, world_width,
                 bitmap_bounds_offsets, bitmap_width, bitmap_height, pos_x, pos_y, speed):
        self.id = player_id

        self.screen_width  = screen_width   # In pixels
        self.screen_height = screen_height  # In pixels

        # Set the scale factor based on device orientation in order to have similar world_width in both modes
        #scale_factor = if (screen_width < screen_height) 1.0 else 0.4

        # Calculate PPM based on the height of the screen to have the same proportion vertically & scale factor
        #self.ppm = screen_height / (world_height * scale_factor)

        # Calculate PPM based on the screenWidth of the screen to have the same proportion horizontally
        self.ppm = screen_width / world_width

        # Calculate PPM based on the screenHeight of the screen to have the same proportion vertically
        #self.ppm = screen_height / world_height

        # DEBUG
        print("player id: ", player_id)
        print("ppm: ", self.ppm)

        current_world_width  = screen_width / self.ppm
        current_world_height = screen_height / self.ppm

        print("world_width: ", world_width)
        print("current_world_width = screen_width / ppm: ", current_world_width)
        #print("world_height: ", world_height)
        print("current_world_height = screen_height / ppm: ", current_world_height)


        self.world_height = max(screen_width, screen_height) / self.ppm

        self.game_object = GameObject(player_id, bitmap_bounds_offsets, bitmap_width, bitmap_height, pos_x, pos_y, speed)

        # Get bitmap scale
        self.bitmap_width_scale  = bitmap_width * self.ppm / screen_width
        self.bitmap_height_scale = bitmap_height * self.ppm / screen_height


        # Forse queste posso tenerle solo nel client
        self.bitmap_rotation = 8.0

        self.rotation_increment = 0.5
        self.max_rotation = 10.0
        self.min_rotation = -8.0
        #=============================

    # Getters
    def getId(self):
        return self.id

    def getScreenWidth(self):
        return self.screen_width

    def getScreenHeight(self):
        return self.screen_height

    def getPPM(self):
        return self.ppm

    def getWorldHeight(self):
        return self.world_height

    def getBitmapWidthScale(self):
        return self.bitmap_width_scale

    def getBitmapHeightScale(self):
        return self.bitmap_height_scale

    #def getBitmapId(self):
    #    return self.game_object.getId()

    def getBounds(self):
        return self.game_object.getBounds()

    def getBitmapWidth(self):
        return self.game_object.getWidth()

    def getBitmapHeight(self):
        return self.game_object.getHeight()

    def getX(self):
        return self.game_object.getX()  # In meters

    def getY(self):
        return self.game_object.getY()  # In meters

    def getSpeed(self):
        return self.game_object.getSpeed()  # In meters

    def getRotation(self):
            return self.bitmap_rotation  # In degrees

    # Setters

    # Set common world height for the players
    # based on the height of the larger screen in portrait orientation
    # (ISSUE: if two very different resolutions, one player can see its object
    # go down the margin of the screen even if in portrait orientation)
    # SOLUTION: use two different world heights for the players, no issue
    # because it grows towards the end of the screen
    #def setWorldHeight(self, world_height):
    #    self.world_height = world_height  # In meters

    def setX(self, pos_x):
        self.game_object.setX(pos_x)  # In meters

    def setY(self, pos_y):
        self.game_object.setY(pos_y)  # In meters

    def setSpeed(self, speed):
        self.game_object.setSpeed(speed)  # In meters

    # Update UI
    def update(self, dt):
        #self.game_object.update(dt)

        # Set player Y based on last player input
        self.setY( self.getY() + self.getSpeed() * dt )

        # Constrain the bitmap in the screen
        # (based on the height of the larger screen in portrait orientation)
        if self.getY() < 0.0:
            self.setY(0.0)
        elif self.getY() + self.getBitmapHeight() > self.getWorldHeight():
            self.setY( self.getWorldHeight() - self.getBitmapHeight() )

        # Constrain the rotation of the bitmap
        if self.getSpeed() > 0.0 and self.bitmap_rotation <= self.max_rotation:
            self.bitmap_rotation += self.rotation_increment
        elif self.getSpeed() < 0.0 and self.bitmap_rotation >= self.min_rotation:
            self.bitmap_rotation -= self.rotation_increment

        self.game_object.update(dt)

    # Change player device's orientation
    def changeOrientation(self):
        new_height = self.screen_width
        self.screen_width  = self.screen_height
        self.screen_height = new_height
