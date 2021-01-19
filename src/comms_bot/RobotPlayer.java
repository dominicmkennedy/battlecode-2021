package comms_bot;
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

    static final String[] code = {
        "enemy_ec",
        "friendly_ec",
        "enemy_muck",
        "friendly_muck",
        Direction.NORTH.toString(),
        Direction.SOUTH.toString(),
        Direction.WEST.toString(),
        Direction.EAST.toString(),
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
        MapLocation location = rc.getLocation();
        RobotType toBuild = randomSpawnableRobotType();
        int influence = 50;
        int currentInf = 150;
        int income = 0;
        int numScouts = 0;
        int numDefenders = 0;
        int currentFlag = 0;
        int senseRadius = rc.getType().sensorRadiusSquared;
        int infInSlanders = 0;
        int roundNum = rc.getRoundNum();
        Set<Integer> muckIDs = new HashSet<Integer>();
        Set<Integer> flags = new HashSet<Integer>();
        int nearestEnemyMuck = 64;
        int distToEC = 9999;
        int nearestEnemyEC = 0;
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(2);
        MapLocation reference = rc.getLocation();
        int previousBid = 1;
        int previousVotes = 0;
        int bidLimit = 5;
        int bidAmt = 1;


        while (true) {
            roundNum = rc.getRoundNum();
            // Check flags of all muckScouts
            for (int id : muckIDs) {
                if (rc.canGetFlag(id)) {
                    currentFlag = rc.getFlag(id);
                    if (currentFlag != 0) {
                        if (readPrefix(currentFlag) == "enemy_ec") {
                            flags.add(currentFlag);
                        } else if (readPrefix(currentFlag) == "friendly_ec" && flags.contains(currentFlag - 16384)) {
                            flags.remove(currentFlag - 16384);
                        } else if (readPrefix(currentFlag) == "enemy_muck" && moveableDistance(readFlag(currentFlag, location), location) < nearestEnemyMuck) {
                            nearestEnemyMuck = moveableDistance(readFlag(currentFlag, location), location);
                        }
                    }
                } else {
                    muckIDs.remove(id);
                }
            }

            numDefenders = 0;
            for (RobotInfo bot : rc.senseNearbyRobots()) {
                if (bot.getType() == RobotType.MUCKRAKER && bot.getTeam() == rc.getTeam().opponent()) {
                    nearestEnemyMuck = 0;
                } else if (bot.getType() == RobotType.POLITICIAN && bot.getTeam() == rc.getTeam()) {
                    numDefenders++;
                }
            }

            // Calculate decision metrics
            if (roundNum % 150 == 0) {
                infInSlanders = (int) (infInSlanders/3);
            }

            distToEC = 9999;
            for (int flag : flags) {
                if (moveableDistance(readFlag(flag, location), location) < distToEC) {
                    distToEC = moveableDistance(readFlag(flag, location), location);
                    nearestEnemyEC = flag;
                }
            }

            income = rc.getInfluence() - currentInf;
            currentInf = currentInf + income;
            numScouts = muckIDs.size();

            // Decide what robot to build and build it
            if (roundNum <= 3) {
                if (tryBuild(RobotType.SLANDERER, 41)) {
                    infInSlanders = infInSlanders + 41;
                }
            } else if (roundNum < 35 && numScouts < 16) {
                tryBuild(RobotType.MUCKRAKER, 1);
            } else if (roundNum < 45 && numDefenders < 4) {
                tryBuild(RobotType.POLITICIAN, 15);
            } else if (currentInf < 500 && income < 10 && nearestEnemyMuck > 15 && infInSlanders < 300) {
                if (tryBuild(RobotType.SLANDERER, 41)) {
                    infInSlanders = infInSlanders + 41;
                } else tryBuild(RobotType.SLANDERER, 21);
                    infInSlanders = infInSlanders + 21;
            } else if (numDefenders < 4) {
                tryBuild(RobotType.POLITICIAN, 15);
            } else if (currentInf < 50) {
                ;
            } else if (nearestEnemyEC != 0) {
                tryBuild(RobotType.POLITICIAN, 50);
                if (rc.canSetFlag(nearestEnemyEC));
                    rc.setFlag(nearestEnemyEC);
            } else if (numScouts < 16) {
                tryBuild(RobotType.MUCKRAKER, 1);
            }



            // Add new scouts to scout set
            nearbyBots = rc.senseNearbyRobots(2);
            for (RobotInfo bot : nearbyBots) {
                if (bot.getType() == RobotType.MUCKRAKER && bot.getTeam() == rc.getTeam() && rc.getFlag(bot.getID()) == 0) {
                    muckIDs.add(bot.getID());
                }
            }

            // Bid
            if (rc.getInfluence() < 100) {
                bidLimit = Math.min(2, rc.getInfluence());
            } else if (rc.getInfluence() < 1000) {
                bidLimit = (int) (Math.round(rc.getInfluence()/2));
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
            Clock.yield();
        }
    }

    static void runPolitician() throws GameActionException {
        MapLocation destination = new MapLocation(0, 0); // Change this to a random location within some radius of the starting location
        int id = rc.getID();
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] nearbyBots = rc.senseNearbyRobots();
        MapLocation ecLoc = rc.getLocation();
        int ecID = 0;
        int ecFlag = 0;

        while (true) {
            // Get info from home EC base
            for (RobotInfo bot : nearbyBots) {
                if (bot.getType() == RobotType.ENLIGHTENMENT_CENTER && bot.getTeam() == rc.getTeam()) {
                    ecLoc = bot.getLocation();
                    ecID = bot.getID();
                    ecFlag = rc.getFlag(ecID);
                }
            }

            // Differentiate class
            if (rc.getInfluence() < 20) {
                runDefense();
                return;
            } else if (ecFlag != 0) {
                destination = readFlag(ecFlag, ecLoc);
                runAttacker(destination);
                return;
            }

            MapLocation location = rc.getLocation();
            int actionRadius = rc.getType().actionRadiusSquared;
            RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
            Direction direction = location.directionTo(destination);

            if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
                return;
            } 

            if (rc.canSetFlag(10)) {
                rc.setFlag(10);
            }
            tryMove(randomDirection());


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
                            destination = readFlag(rc.getFlag(bot.getID()), bot.getLocation());
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
            // Empower if muckrakers nearby or good empowerment buff
            if (rc.getEmpowerFactor(rc.getTeam(), 0) > 3) {
                if (rc.canEmpower(actionRadius)) {
                    rc.empower(actionRadius);
                }
            }
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

    static void runSlanderer() throws GameActionException {
        int bornOn = rc.getRoundNum();
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(2, rc.getTeam());
        MapLocation ecLoc = rc.getLocation();

        for (RobotInfo bot : nearbyBots) {
            if (bot.type == RobotType.ENLIGHTENMENT_CENTER) {
                ecLoc = bot.location;
            }
        }
        while (true) {
            //sense nearby enemies
            Team enemy = rc.getTeam().opponent();
            MapLocation location = rc.getLocation();
            MapLocation enemyLoc = rc.getLocation();
            MapLocation friendLoc = rc.getLocation();
            Direction dir = Direction.CENTER;

            for (RobotInfo robot : rc.senseNearbyRobots()) {
                if (robot.getType() == RobotType.MUCKRAKER && robot.getTeam() == enemy) {
                    enemyLoc = robot.getLocation();
                } else if (robot.getType() == RobotType.SLANDERER && robot.getTeam() == rc.getTeam()) {
                    friendLoc = robot.getLocation();
                }
            }

            if (!enemyLoc.equals(location)) {
                dir = enemyLoc.directionTo(location);
                tryMove(dir);
            } else if (location.distanceSquaredTo(ecLoc) < 12) {
                dir = ecLoc.directionTo(location);
                if (tryMove(dir)) {
                    ;
                } else if (tryMove(dir.rotateRight())) {
                    ;
                } else tryMove(dir.rotateLeft());
            } else if (location.distanceSquaredTo(ecLoc) < 30) {
                dir = location.directionTo(ecLoc);
            } else if (!friendLoc.equals(location)) {
                dir = location.directionTo(friendLoc);
                tryMove(dir);
            }

            if (rc.getRoundNum() - bornOn > 300) {
                runPolitician();
            }

            Clock.yield();
        }
    }

    static void runMuckraker() throws GameActionException {
        
        runMuckScout();
    }

    static void runMuckScout() throws GameActionException {
        Set<String> seenEdges = new HashSet<String>();
        MapLocation location = rc.getLocation();
        RobotInfo[] nearbyBots = rc.senseNearbyRobots();
        RobotInfo nearestFriendlyMuck = rc.senseRobot(rc.getID());
        int dist = 9999;
        Direction dir = Direction.CENTER;
        Direction checkDir = Direction.CENTER;
        MapLocation ecLoc = rc.getLocation();
        int flag = 0;
        int enemyMuckDist = 9999;

        for (RobotInfo bot : nearbyBots) {
            if (bot.getType() == RobotType.ENLIGHTENMENT_CENTER && bot.getTeam() == rc.getTeam()) {
                ecLoc = bot.getLocation();
                break;
            }
        }

        while (true) {
            dir = Direction.CENTER;
            dist = 9999;
            enemyMuckDist = 9999;
            nearbyBots = rc.senseNearbyRobots();
            for (RobotInfo bot : nearbyBots) {
                if (rc.canExpose(bot.getID())) {
                    rc.expose(bot.getID());
                } else if (bot.getType() == RobotType.SLANDERER && bot.getTeam() != rc.getTeam()) {
                    dir = location.directionTo(bot.getLocation());
                } else if (bot.getType() == RobotType.MUCKRAKER) {
                    if (bot.getTeam() == rc.getTeam() && rc.getLocation().distanceSquaredTo(bot.getLocation()) <= dist) {
                        nearestFriendlyMuck = bot;
                        dist = rc.getLocation().distanceSquaredTo(bot.getLocation());
                    } else if (bot.getTeam() == rc.getTeam().opponent() && moveableDistance(ecLoc, bot.getLocation()) <= enemyMuckDist) {
                        enemyMuckDist = Math.max(Math.abs(ecLoc.x - bot.getLocation().x), Math.abs(ecLoc.y - bot.getLocation().y));
                        flag = writeFlag(ecLoc, bot.getLocation(), "enemy_muck");
                        if (rc.canSetFlag(flag)) {
                            rc.setFlag(flag);
                        }
                    }
                } else if (bot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    if (bot.getTeam() != rc.getTeam()) {
                        flag = writeFlag(ecLoc, bot.getLocation(), "enemy_ec");
                        if (rc.canSetFlag(flag)) {
                            rc.setFlag(flag);
                            break;
                        }
                    } else if (bot.getTeam() == rc.getTeam()) {
                        flag = writeFlag(ecLoc, bot.getLocation(), "friendly_ec");
                        if (rc.canSetFlag(flag)) {
                            rc.setFlag(flag);
                            break;
                        }
                    }
                }
            }
            dir = nearestFriendlyMuck.getLocation().directionTo(rc.getLocation()); // Move away from the closest friendly muckraker


            // Detect edges
            location = rc.getLocation();
            for (int i = 0; i < 4; i++) {
                checkDir = directions[2*i];
                if (!rc.onTheMap(location.add(checkDir)) && !seenEdges.contains(checkDir)) {
                    flag = writeFlag(ecLoc, location, checkDir.toString());
                    dir = checkDir.opposite();
                    if (rc.canSetFlag(flag)) {
                        rc.setFlag(flag);
                        seenEdges.add(checkDir.toString());
                    }
                }
            }

            if (tryMove(dir)) {
                ;
            } else if (tryMove(randomDirection())) {
                ;
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
     * Write information into a flag
     *
     * @return a the location indicated by the flag (for now)
     */
    static int writeFlag(MapLocation reference, MapLocation target, String preface) {
        int dx = target.x - reference.x;
        int dy = target.y - reference.y;
        String xCoord = String.format("%6s", Integer.toBinaryString(Math.abs(dx))).replace(' ', '0');
        String yCoord = String.format("%6s", Integer.toBinaryString(Math.abs(dy))).replace(' ', '0');
        String header = "";

        if (dx < 0) {
            xCoord = "0" + xCoord;
        } else {
            xCoord = "1" + xCoord;
        }
        if (dy < 0) {
            yCoord = "0" + yCoord;
        } else {
            yCoord = "1" + yCoord;
        }

        for (int i = 0; i < code.length; i++) {
            if (preface == code[i]) {
                header = String.format("%10s", Integer.toBinaryString(i)).replace(' ', '0');
                break;
            }
        }

        int flag = Integer.parseInt(header + xCoord + yCoord, 2);

        return flag;
    }

    /**
     * Return prefix from flag
     *
     * @return a string indicating what is at the location designated by the flag
     */
    static String readPrefix(int flag) {
        String flagAsString = String.format("%24s", Integer.toBinaryString(flag)).replace(' ', '0');
        String preface = code[Integer.parseInt(flagAsString.substring(0,10), 2)];
        return preface;
    }

    /**
     * Read information from a flag
     *
     * @return a the location indicated by the flag (for now)
     */
    static MapLocation readFlag(int flag, MapLocation ecLoc) {
        String flagAsString = String.format("%24s", Integer.toBinaryString(flag)).replace(' ', '0');
        String preface = code[Integer.parseInt(flagAsString.substring(0,10), 2)];
        int xCoord = Integer.parseInt(flagAsString.substring(11,17), 2);
        int yCoord = Integer.parseInt(flagAsString.substring(18,24), 2);

        if (Integer.parseInt(flagAsString.substring(10,11)) == 0) {
            xCoord = -xCoord;
        }
        if (Integer.parseInt(flagAsString.substring(17,18)) == 0) {
            yCoord = -yCoord;
        }
        return new MapLocation(ecLoc.x + xCoord, ecLoc.y + yCoord);
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
     * Returns moveable distance between two MapLocations
     *
     * @return a integer distance
     */
    static int moveableDistance(MapLocation loc1, MapLocation loc2) {
        return Math.max(Math.abs(loc1.x - loc2.x), Math.abs(loc1.y - loc2.y));
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
     * Attempts to build the given robot with the given influence.
     *
     * @param  toBuild intended robot
     * @return true if a build was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType toBuild, int influence) throws GameActionException {
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
                return true;
            }
        }

        return false;
    }
}
