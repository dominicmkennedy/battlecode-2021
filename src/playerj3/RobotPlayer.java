package playerj3;
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
        Direction.SOUTHEAST,
        Direction.WEST,
        Direction.NORTHEAST,
        Direction.SOUTH,
        Direction.NORTHWEST,
        Direction.EAST,
        Direction.SOUTHWEST,
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
        int numDefenders = 0;
        int numMucks = 0;
        RobotInfo[] nearbyBots = rc.senseNearbyRobots();
        Boolean canBuild = false;
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        Set<Integer> flags = new HashSet<Integer>();
        int previousBid = 1;
        int previousVotes = 0;
        int bidLimit = 5;

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
                            flag = rc.getFlag(id);
                            rc.setFlag(flag);  // In future iteration, make it so that it stores all new flags and deals with them
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

            // Check how many defenders you have and how many enemy muckrakers are close
            numMucks = 0;
            numDefenders = 0;
            nearbyBots = rc.senseNearbyRobots();
            for (RobotInfo bot : nearbyBots) {
                if (bot.type == RobotType.MUCKRAKER && bot.team == rc.getTeam().opponent()) {
                    numMucks++;
                } else if (bot.type == RobotType.POLITICIAN && bot.team == rc.getTeam() && bot.influence < 50) {
                    numDefenders++;
                }
            }

            // Decide toBuild and influence (and direction) based on round number
            if (roundNum == 1) {
                toBuild = RobotType.SLANDERER;
                influence = 130;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        break;
                    }
                }
            } else if (roundNum <= 51 && influence > 63) {
                toBuild = RobotType.SLANDERER;
                influence = 63;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        break;
                    }
                }
            } else if (numDefenders < 4 || numDefenders < numMucks) {
                toBuild = RobotType.POLITICIAN;
                influence = 20;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                    }
                }
            } else if (i < 16 && flag == 0) {
                toBuild = RobotType.MUCKRAKER;
                influence = 1;
                for (int k=0; k<8; k++) {
                    if (rc.canBuildRobot(toBuild, directions[(roundNum + k)%8], influence)) {
                        rc.buildRobot(toBuild, directions[(roundNum + k)%8], influence);
                        i++;
                    }
                }
            } else if (rc.getFlag(rc.getID()) != 0 && roundNum < 2000) {
                toBuild = RobotType.POLITICIAN;
                influence = 50;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        break;
                    }
                }
            } else {
                toBuild = randomSpawnableRobotType();
                influence = 41;
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        break;
                    }
                }
            }


            // Add new bots to the id roster
            for (RobotInfo bot : rc.senseNearbyRobots(8, rc.getTeam())) {
                if (!idToFlag.containsKey(bot.getID()) && bot.getType() == RobotType.MUCKRAKER) {
                    idToFlag.put(bot.getID(),rc.getFlag(bot.getID()));
                }
            }


            // Bad bid code. Fix this
            if (roundNum < 500) {
                bidLimit = Math.min(5,rc.getInfluence());
            } else {
                bidLimit = (int) (Math.round((rc.getInfluence()*9)/10));
            }

            if (rc.getTeamVotes() > previousVotes) {
                bidAmt = Math.min((int)(Math.round((previousBid*7)/8)),bidLimit);
            } else {
                bidAmt = Math.min((int)(previousBid*2),bidLimit);
            }

            previousVotes = rc.getTeamVotes();

            if (rc.getTeamVotes() <= 750) {
                if (rc.canBid(bidAmt)) {
                    rc.bid(bidAmt);
                    previousBid = bidAmt;
                } else if (rc.canBid(1)) {
                    rc.bid(1);
                    previousBid = 1;
                }
            }
            // System.out.println("Bytecode used: " + Clock.getBytecodeNum());
            Clock.yield();
        }

    }


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
        if (rc.getInfluence() == 20) {
            runDefense();
            return;
        } else if (ecFlag != 0) {
            destination = interpretFlag(ecFlag, ecLoc);
            runAttacker(destination);
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

    static void runDefense() throws GameActionException {
        MapLocation location = rc.getLocation();
        MapLocation ecLoc = rc.getLocation();
        int actionRadius = rc.getType().actionRadiusSquared;
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] friendly = rc.senseNearbyRobots(2, rc.getTeam());

        for (RobotInfo bot : friendly) {
            if (bot.type == RobotType.ENLIGHTENMENT_CENTER) {
                ecLoc = bot.location;
            }
        }

        while (true) {
            // Attack Muckrakers
            attackable = rc.senseNearbyRobots(ecLoc, 2, enemy);
            for (RobotInfo bot : attackable) {
                if (bot.type == RobotType.MUCKRAKER || bot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    if (rc.canEmpower(actionRadius)) {
                        rc.empower(actionRadius);
                    }
                }
            }
            // Move
            if (rc.getLocation().distanceSquaredTo(ecLoc) < 6 && tryMove(ecLoc.directionTo(rc.getLocation()))) {
                ;
            } else if (rc.getLocation().distanceSquaredTo(ecLoc) > 10 && tryMove(rc.getLocation().directionTo(ecLoc))) {
                ;
            } else if (rc.getLocation().distanceSquaredTo(ecLoc) <= 2) {
                tryMove(randomDirection());
            }

            Clock.yield();
        }

    }

    static void runAttacker(MapLocation destination) throws GameActionException {
        MapLocation location = rc.getLocation();
        int actionRadius = rc.getType().actionRadiusSquared;
        int senseRadius = rc.getType().sensorRadiusSquared;
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

            if (rc.canSenseLocation(destination)) {
                for (RobotInfo bot : rc.senseNearbyRobots(destination, 0, rc.getTeam())) {
                    if (bot.getTeam() == rc.getTeam() && rc.canGetFlag(bot.getID())) {
                        if (rc.getFlag(bot.getID()) != 0) {
                            destination = interpretFlag(rc.getFlag(bot.getID()), bot.getLocation());
                        } else {
                            runPolitician();
                        }
                    break;
                    }
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
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(2, rc.getTeam());
        MapLocation ecLoc = rc.getLocation();

        for (RobotInfo bot : nearbyBots) {
            if (bot.type == RobotType.ENLIGHTENMENT_CENTER) {
                ecLoc = bot.location;
            }
        }
        // makes one move away from the ec
        while (rc.getLocation().distanceSquaredTo(ecLoc) <= 2) {
            tryMove(randomDirection());
            Clock.yield();
        }
       
        while (true) {
            //sense nearby enemies
            Team enemy = rc.getTeam().opponent();
            MapLocation enemyLoc = rc.getLocation();
            for (RobotInfo robot : rc.senseNearbyRobots(20, enemy)) {
                if (robot.type == RobotType.MUCKRAKER) {
                    enemyLoc = robot.location;
                }
            }
            if (!enemyLoc.equals(rc.getLocation())) {
                Direction dir = enemyLoc.directionTo(rc.getLocation());
                tryMove(dir);
            }
            Clock.yield();
        }
    }

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;

        runMuckScout();

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

    static void runMuckScout() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        MapLocation ecLoc = rc.getLocation();
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(2);
        MapLocation flare = rc.getLocation();
        Direction bias = Direction.CENTER;
        int flag = 0;
        int ecID = 0;
        int ecFlag = 0;
        int actionRadius = 12;
        int senseRadius = 30;

        // Set Home ec location, bias, etc.
        rc.setFlag(flag);
        for (RobotInfo bot : nearbyBots) {
            if (bot.getType() == RobotType.ENLIGHTENMENT_CENTER && bot.getTeam() == rc.getTeam()) {
                ecLoc = bot.getLocation();
                ecID = bot.getID();
                ecFlag = rc.getFlag(ecID);
                bias = directions[rc.getRoundNum()%8];
                // bias = ecLoc.directionTo(rc.getLocation());
            }
        }

        while (true) {
            Direction dir = Direction.CENTER;
            // Sense, Expose, Flag
            nearbyBots = rc.senseNearbyRobots(senseRadius); // Make this pick the most influential slanderer
            for (RobotInfo bot : nearbyBots) {
                if (rc.canExpose(bot.getID())) {
                    rc.expose(bot.getID());
                } else if (bot.type.canBeExposed() && bot.getTeam() == enemy) {
                    dir = rc.getLocation().directionTo(bot.getLocation());
                } else if (bot.getType() == RobotType.MUCKRAKER && bot.getTeam() == rc.getTeam()) {
                    dir = rc.getLocation().directionTo(bot.getLocation()).opposite();
                } else if (bot.getType() == RobotType.ENLIGHTENMENT_CENTER && bot.getTeam() != rc.getTeam()) {
                    flag = writeFlag(ecLoc, bot.getLocation());
                    if (rc.canSetFlag(flag)) {
                        rc.setFlag(flag);
                    }
                }
            }

            // Add something to deal with edges
            for (Direction d : directions) {
                if (!rc.onTheMap(rc.adjacentLocation(d))) {
                    bias = d.opposite();
                }
            }

            // Move (away from friendly muckrakers, towards enemy slanderers, randomly)
            if (dir != Direction.CENTER && tryMove(dir)) {
                ;
            } else if (bias != Direction.CENTER) {
                tryMove(bias);
            } else if (tryMove(randomDirection())) {
                ;
            } else {
                for (Direction d : directions) {
                    if (tryMove(d)) {
                        break;
                    }
                }
            }

            Clock.yield();
        }
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
     * Write information into a flag
     *
     * @return a the location indicated by the flag (for now)
     */
    static int writeFlag(MapLocation ecLoc, MapLocation destination) {
        int flag = 100 * (Math.abs(destination.x - ecLoc.x));
        flag += (Math.abs(destination.y - ecLoc.y));
        flag += 10000 * (int) ((Math.signum(destination.y - ecLoc.y)+1)/2);
        flag += 100000 * (int) ((Math.signum(destination.x - ecLoc.x)+1)/2);
        return flag;
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
