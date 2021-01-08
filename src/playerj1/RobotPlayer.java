package playerj1;
import battlecode.common.*;
import java.lang.Math;
import java.util.*;

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
        int flag = 0;
        int roundNum = rc.getRoundNum();
        Direction direction = Direction.NORTH;
        Hashtable<Integer, Integer> idToFlag = new Hashtable<Integer, Integer>();
        int i = 0;
        Boolean canBuild = false;
        ArrayList<Integer> toRemove = new ArrayList<Integer>();

        rc.setFlag(flag);

        while (true) {
            roundNum = rc.getRoundNum();
            // Check whether flags have changed
            for (int id : idToFlag.keySet()) {
                if (!rc.canGetFlag(id)) {
                    toRemove.add(id);
                } else if (idToFlag.containsKey(id)) {
                    if (rc.getFlag(id) != idToFlag.get(id)) {
                        if (rc.canSetFlag(rc.getFlag(id))) {
                            rc.setFlag(rc.getFlag(id));  // In future iteration, make it so that it stores all new flags and deals with them
                            break;
                        }
                        idToFlag.put(id,rc.getFlag(id));
                    }
                }
            }

            // Remove unreachable ids from idToFlag
            for (int j = 0; j < toRemove.size(); j++) {
                idToFlag.remove(toRemove.get(j));
            }

            // Decide toBuild and influence (and direction) based on round number
            if (roundNum%50==0) {
                toBuild = RobotType.SLANDERER;
                influence = rc.getInfluence() - rc.getInfluence()%20;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        break;
                    }
                }
            } else if (i < 24) {
                toBuild = RobotType.POLITICIAN;
                influence = 1;
                if (rc.canBuildRobot(toBuild,directions[i%8], influence)) {
                    rc.buildRobot(toBuild, directions[i%8], influence);
                    i++;
                }
            } else if (roundNum%10 == 0) {
                toBuild = RobotType.MUCKRAKER;
                influence = 2;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        break;
                    }
                }
            } else if (rc.getFlag(rc.getID()) != 0 && roundNum < 2000) {
                toBuild = RobotType.POLITICIAN;
                influence = 100;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        break;
                    }
                }
            } else {
                toBuild = randomSpawnableRobotType();
                influence = 20;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        break;
                    }
                }
            }


            // Add new bots to the id roster
            for (RobotInfo bot : rc.senseNearbyRobots(8, rc.getTeam())) {
                if (!idToFlag.containsKey(bot.getID()) && bot.getType() == RobotType.POLITICIAN) {
                    idToFlag.put(bot.getID(),rc.getFlag(bot.getID()));
                }
            }


            // Generic bid code
            if (roundNum > 1500) {
                bidAmt = (int)Math.ceil(rc.getInfluence()/(3000-roundNum) + 1);
            } else if (rc.getInfluence() < 100) {
                bidAmt = Math.max(6,rc.getInfluence());
            } else {
                bidAmt = (int)(rc.getInfluence()/10);
            }


            if (rc.canBid(bidAmt)) {
                rc.bid(bidAmt);
            } else if (rc.canBid(1)) {
                rc.bid(1);
            }
            // System.out.println("Bytecode used: " + Clock.getBytecodeNum());
            Clock.yield();
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
        RobotInfo[] nearbyBots = rc.senseNearbyRobots();
        MapLocation ecLoc = rc.getLocation();
        int ecID = 0;
        int ecFlag = 0;

        // Get info from home EC base
        for (RobotInfo bot : nearbyBots) {
            if (bot.getType() == RobotType.ENLIGHTENMENT_CENTER && bot.getTeam() == rc.getTeam()) {
                ecLoc = bot.getLocation();
                ecID = bot.getID();
                ecFlag = rc.getFlag(ecID);
            }
        }

        // Differentiate class
        if (ecFlag != 0) {
            destination = interpretFlag(ecFlag, ecLoc);
            runAttacker(destination);
            return;
        } else if (rc.getRoundNum() <= 100) {
            runScout();
            return;
        }

        while (true) {
            MapLocation location = rc.getLocation();
            int actionRadius = rc.getType().actionRadiusSquared;
            RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
            Direction direction = location.directionTo(destination);

            if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
                return;
            } 

            tryMove(randomDirection());


            Clock.yield();
        }
    }

    static void runScout() throws GameActionException {
        MapLocation ecLoc = rc.getLocation();
        Direction bias = Direction.NORTH;
        RobotInfo[] nearbyBots = rc.senseNearbyRobots();
        MapLocation flare = rc.getLocation();
        int flag = 0;

        rc.setFlag(flag);
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

    static void runAttacker(MapLocation destination) throws GameActionException {
        MapLocation location = rc.getLocation();
        int actionRadius = rc.getType().actionRadiusSquared;
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] convertible = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        Direction direction = location.directionTo(destination);
        Boolean nearEC = false;

        while (true) {
            location = rc.getLocation();
            direction = location.directionTo(destination);
            attackable = rc.senseNearbyRobots(actionRadius, enemy);
            convertible = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
            for (RobotInfo bot : attackable) {
                if (bot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    nearEC = true;
                    break;
                }
            }
            if (rc.canEmpower(actionRadius)) {
                if (nearEC || convertible.length != 0) {
                    rc.empower(actionRadius);
                    return;
                }
            }

            if (tryMove(direction)) {
                ;
            } else if (tryMove(direction.rotateLeft())) {
                ;
            } else if (tryMove(direction.rotateRight())) {
                ;
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

    /**
     * Interpret information from a flag
     *
     * @return a the location indicated by the flag (for now)
     */
    static MapLocation interpretFlag(int flag, MapLocation ecLoc) {
        int xCoord = 0;
        int yCoord = 0;

        yCoord = flag % 100;
        xCoord = ((int) (flag/100)) % 100;
        if (flag < 100000) {
            xCoord = -xCoord;
            if (flag < 10000) {
                yCoord = -yCoord;
            }
        } else if (flag - 100000 < 10000) {
            yCoord = -yCoord;
        }

        return new MapLocation(ecLoc.x + xCoord, ecLoc.y + yCoord);
    }
}
