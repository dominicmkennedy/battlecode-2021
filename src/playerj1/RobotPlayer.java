package playerj1;
import battlecode.common.*;
import java.lang.Math;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static int turnCount;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        //System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();
        int influence = 50;
        int bidAmt = 1;

        if (rc.getRoundNum() == 0) {
            toBuild = RobotType.SLANDERER;
            influence = 140;
        }

        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }

        if (rc.canBid(bidAmt)) {
            rc.bid(bidAmt);
        }
        Clock.yield();

        int i = 0;
        if (rc.getRoundNum() <= 50) {
            toBuild = RobotType.POLITICIAN;
            influence = 1;
            while (i < 24) {
                if (rc.canBuildRobot(toBuild, directions[i%8], influence)) {
                    rc.buildRobot(toBuild, directions[i%8], influence);
                    i++;
                }
                if (rc.canBid(bidAmt)) {
                    rc.bid(bidAmt);
                }
                Clock.yield();
            }
        }

        if (rc.getRoundNum()%50 == 0 && rc.getRoundNum() < 1000) {
            toBuild = RobotType.SLANDERER;
            influence = rc.getInfluence() - rc.getInfluence()%20;
        }

        
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }

        bidAmt = (int)Math.ceil(rc.getInfluence()/(3000-rc.getRoundNum()) + 1);
        if (rc.canBid(bidAmt)) {
            rc.bid(bidAmt);
        } else if (rc.canBid(1)) {
            rc.bid(1);
        }

    }
    /**
    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }
    */
    static void runPolitician() throws GameActionException {
        MapLocation destination = new MapLocation(30064, 30064); // Change this to a random location within some radius of the starting location
        int id = rc.getID();
        Team enemy = rc.getTeam().opponent();


        if (rc.getRoundNum() <= 100) {
            runScout();
        }

        while (true) {
            MapLocation location = rc.getLocation();
            int actionRadius = rc.getType().actionRadiusSquared;
            RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
            Direction direction = location.directionTo(destination);

            if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
                //System.out.println("empowering...");
                rc.empower(actionRadius);
                //System.out.println("empowered");
                return;
            }
            if (tryMove(direction)) {
                ;
            } else {
                tryMove(randomDirection());
            }
            Clock.yield();
        }
    }

    static void runScout() throws GameActionException {
        MapLocation ecLoc = rc.getLocation();
        Direction bias = Direction.NORTH;
        RobotInfo[] nearbyBots = rc.senseNearbyRobots();
        MapLocation flare = rc.getLocation();
        int flag = 0;

        for (RobotInfo bot : nearbyBots) {
            if (bot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ecLoc = bot.getLocation();
                bias = ecLoc.directionTo(rc.getLocation());
            }
        }

        while (true) {
            // Move
            if (!rc.isReady()) {
                continue;
            } else if (Math.random() > 0.5) {
                tryMove(bias);
            } else {
                tryMove(randomDirection());
            }
            // Check for foreign ECs
            nearbyBots = rc.senseNearbyRobots();
            for (RobotInfo bot : nearbyBots) {
                if (bot.getTeam() != rc.getTeam() && bot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    flare = bot.getLocation();  // Set flag
                    flag = 100 * (Math.abs(flare.x - ecLoc.x));
                    flag += (Math.abs(flare.y - ecLoc.y));
                    flag += 10000 * (int) ((Math.signum(flare.y - ecLoc.y)+1)/2);
                    flag += 100000 * (int) ((Math.signum(flare.x - ecLoc.x)+1)/2);
                    if (rc.canSetFlag(flag)) {
                        rc.setFlag(flag);
                    }
                }
            }

            Clock.yield();
        }
        
    }

    static void runSlanderer() throws GameActionException {
        if (tryMove(Direction.NORTH)) {
            ;
        } else if (tryMove(randomDirection())) {
            ;
        }
    } 

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    //System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection()))
            ;
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        //System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}
