package dom2;
import battlecode.common.*;

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
        //e center will first bid on a vote
        //bid will be calculated at
        //(rounds^0.6)
        int votes = rc.getTeamVotes();
        int totalInfluence = rc.getInfluence();
        int round = rc.getRoundNum();
        int bid=(int)Math.pow(round, 0.6);
        while (!rc.canBid(bid)) {bid-=2;}
        rc.bid(bid);
        totalInfluence -= bid;


        //e center will then attempt to make a bot bots come at a ratio of
        //55% pol influnce at 1/8 of total, 
        //5% muckraker influnece at 1,
        //40% slanderer influence at 1/16 of total
        int percent = round % 20;
        if (percent < 11) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.POLITICIAN, dir, totalInfluence/8)) {
                    rc.buildRobot(RobotType.POLITICIAN, dir, totalInfluence/8);
                    break;
                } 
            }
        }
        else if (percent == 19) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
                    rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
                    break;
                } 
            }
        }
        else {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.SLANDERER, dir, totalInfluence/16)) {
                    rc.buildRobot(RobotType.SLANDERER, dir, totalInfluence/16);
                    break;
                } 
            }
        }
    }
    

    /*static void runPolitician() throws GameActionException {
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
    }*/
    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        //changed it so that policticions bassiclly ignore MUCKRAKERS
        int worth = attackable.length;
        for (int i=0; i<attackable.length; i++) 
            if (attackable[i].getType() == RobotType.MUCKRAKER) worth--;
        if (worth != 0 && rc.canEmpower(actionRadius)) {
            rc.empower(actionRadius);
            return;
        }
        //still using random movement idrk what's ideal here
        tryMove(randomDirection());
    }

    /*static void runSlanderer() throws GameActionException {
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }*/
    static void runSlanderer() throws GameActionException {
        //slanderers will be put into one of four groups based on bot id
        //each group will attempt to move in diagonal lines across the map
        //when the digonal is unavilible the will move on random
        int oneinfour = rc.getID() % 4;
        if (!tryMove(directions[(2*oneinfour)+1]))
            for (Direction dir : directions) 
                if (tryMove(dir))
                    break;

    }

    /*static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }*/
    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        //really sloppy but these two loops check for to
        //expose the slanderer with the highest influnce first
        int top = 0;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                if (robot.influence > top)
                    top = robot.influence;
            }
        }
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                if (robot.influence == top) {
                    if (rc.canExpose(robot.location)) {
                        rc.expose(robot.location);
                        return;
                    }
                }
            }
        }
        
        //this will check for all bots within the sensing radius 
        //and will attempt to move towards those that can be exposed
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            if (robot.type.canBeExposed()) {
                if (robot.getTeam() != rc.getTeam()) {
                    MapLocation myLoc = rc.getLocation();
                    MapLocation enemyLoc = robot.getLocation();
                    if ((enemyLoc.x == myLoc.x) && (enemyLoc.y > myLoc.y))
                        tryMove(directions[0]);
                    if ((enemyLoc.x == myLoc.x) && (enemyLoc.y < myLoc.y))
                        tryMove(directions[4]);
                    if ((enemyLoc.x > myLoc.x) &&  (enemyLoc.y > myLoc.y))
                        tryMove(directions[1]);
                    if ((enemyLoc.x > myLoc.x) &&  (enemyLoc.y < myLoc.y))
                        tryMove(directions[3]);
                    if ((enemyLoc.x < myLoc.x) &&  (enemyLoc.y > myLoc.y))
                        tryMove(directions[7]);
                    if ((enemyLoc.x < myLoc.x) &&  (enemyLoc.y < myLoc.y))
                        tryMove(directions[5]);
                    if ((enemyLoc.x > myLoc.x) && (enemyLoc.y == myLoc.y))
                        tryMove(directions[2]);
                    if ((enemyLoc.x < myLoc.x) && (enemyLoc.y == myLoc.y))
                        tryMove(directions[4]);
                }
            }
        }
        
        //rn it just moves rondomly otherwise 
        //but id like it to retreat from ALL other bots
        tryMove(randomDirection());
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
