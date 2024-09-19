import io.socket.emitter.Emitter;
import jsclub.codefest2024.sdk.Hero;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;
import jsclub.codefest2024.sdk.model.Inventory;
import jsclub.codefest2024.sdk.model.enemies.Enemy;
import jsclub.codefest2024.sdk.model.equipments.Armor;
import jsclub.codefest2024.sdk.model.equipments.HealingItem;
import jsclub.codefest2024.sdk.model.obstacles.Obstacle;
import jsclub.codefest2024.sdk.model.players.Player;
import jsclub.codefest2024.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Bot {

    public static Bot Instance;

    public int MapSize;
    public Hero hero;


    public Player player;
    private State currentState;
    public Player currentTarget;
    public Node nearestTarget;
    public Node currentItemTarget;
    public Node currentPosition;


    public GameMap gameMap;
    public List<Player> otherPlayers;
    public List<Node> chests;
    public List<Obstacle> restricedList;
    public List<Node> restrictedNodes;
    public List<Node> otherPlayesPosition;

    public Bot(String GameId, String PlayerName, String Key) {
        hero = new Hero(GameId, PlayerName, Key);
        restrictedNodes = new ArrayList<>();
        otherPlayesPosition = new ArrayList<>();
        chests = new ArrayList<>();
    }

    public static Bot GetInstance() {
        if (Instance == null) {
            Instance = new Bot(BotInfo.GAME_ID, BotInfo.PLAYER_NAME, BotInfo.PLAYER_KEY);
        }
        return Instance;
    }

//region Method
    public void InitData() {
        chests.clear();
        player = gameMap.getCurrentPlayer();
        otherPlayers = gameMap.getOtherPlayerInfo();
        restricedList = gameMap.getListIndestructibleObstacles();
        chests.addAll(gameMap.getListChests());
        restricedList.addAll(gameMap.getListTraps());
        if(currentState != State.FindChest) restricedList.addAll(gameMap.getListChests());
        if(currentState != State.FindEnemy) restrictedNodes.addAll(otherPlayers);
        restrictedNodes.clear();
        otherPlayesPosition.clear();

        currentPosition = GetCurrentPosition();

        for (Player p : otherPlayers) {
            if (p.getIsAlive()) {
                otherPlayesPosition.add(new Node(p.getX(), p.getY()));
            }
        }
        for (Obstacle o : restricedList) {
            restrictedNodes.add(new Node(o.getX(), o.getY()));
        }

        for (Enemy e : gameMap.getListEnemies()) {
            int enemyX = e.getX();
            int enemyY = e.getY();

            // Add enemy's position
            restrictedNodes.add(new Node(enemyX, enemyY));

            // Add surrounding positions (top, bottom, left, right)
            restrictedNodes.add(new Node(enemyX - 1, enemyY)); // Left
            restrictedNodes.add(new Node(enemyX + 1, enemyY)); // Right
            restrictedNodes.add(new Node(enemyX, enemyY - 1)); // Down
            restrictedNodes.add(new Node(enemyX, enemyY + 1)); // Up

            // Optionally, add diagonal positions for a full surrounding
            restrictedNodes.add(new Node(enemyX - 1, enemyY - 1)); // Bottom-left
            restrictedNodes.add(new Node(enemyX + 1, enemyY - 1)); // Bottom-right
            restrictedNodes.add(new Node(enemyX - 1, enemyY + 1)); // Top-left
            restrictedNodes.add(new Node(enemyX + 1, enemyY + 1)); // Top-right
        }
    }

    public void Run() throws IOException {
        Emitter.Listener onMapUpdate = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Update(args);
            }
        };

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(BotInfo.SERVER_URL);
    }

    private String GetDirection() {
        if (currentPosition.getX() != currentTarget.getX()) {
            // Move left if the target is to the left
            if (currentPosition.getX() > currentTarget.getX()) return "l";
            // Move right if the target is to the right
            if (currentPosition.getX() < currentTarget.getX()) return "r";
        } else if (currentPosition.getY() != currentTarget.getY()) {
            // Move down if the target is below
            if (currentPosition.getY() > currentTarget.getY()) return "d";
            // Move up if the target is above
            if (currentPosition.getY() < currentTarget.getY()) return "u";
        }

        // If the target is at the same position as the bot, or no valid direction found
        return "";
    }

    private Node GetCurrentPosition() {
        return new Node(player.getX(), player.getY());
    }

    private String GetItemPath(){
        currentItemTarget = GetItemPosition();
        return PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, currentItemTarget, true);
    }

    private Node GetItemPosition() {
        switch (currentState) {
            case FindHealth -> {
                return FindNearest(ItemType.HEALTH);
            }
            case FindArmor -> {
                return FindNearest(ItemType.ARMOR);
            }
            case FindWeapon -> {
                return FindNearest(ItemType.WEAPON);
            }
            case FindChest -> {
                return FindNearest(ItemType.CHEST);
            }
            case FindGun -> {
                return FindNearest(ItemType.GUN);
            }
            case FindMelee -> {
                return FindNearest(ItemType.MELEE_WEAPON);
            }
            case FindThrowable -> {
                return FindNearest(ItemType.THROWABLE_WEAPON);
            }
            default -> {
                return null;
            }
        }
    }

    private boolean SamePosition(Node a, Node b){
        return a.getX() == b.getX() && a.getY() == b.getY();
    }
// endregion

    public void Update(Object... args) {
        try {
            gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);

            InitData();
            if (!player.getIsAlive()) {
                return;
            }

            System.out.println("Current State: " + currentState);
            System.out.println("HP: " + player.getHp());
            System.out.println("Player pos : " + player.x + ", " + player.y);
            System.out.println("Player : " + player + " \n");

            //System.out.println(gameMap.getCurrentPlayer());
            //System.out.println(gameMap.getOtherPlayerInfo());

            RunLogic();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//region Bot Logic

    private void RunLogic() throws IOException {
        Inventory inventory = hero.getInventory();

        //if(hasWeapon)

        System.out.println("Inventory: "
                    + "\nGun: " + inventory.getGun() + " " + (inventory.getGun() == null  && !gameMap.getAllGun().isEmpty())
                    + "\nMelee: " + inventory.getMelee() + " " + (inventory.getMelee().getId().equals("HAND") && !gameMap.getAllMelee().isEmpty())
                    + "\nThrowable: " + inventory.getThrowable() + " " + (inventory.getThrowable() == null  && !gameMap.getAllThrowable().isEmpty()));

        if(inventory.getGun() != null) System.out.println("Gun range: " + inventory.getGun().getRange());

//        System.out.println("GameMap: "
//                            + "\nGun List Size: " + gameMap.getAllGun().size()
//                            + "\nMelee List Size: " + gameMap.getAllMelee().size()
//                            + "\nThrowable List Size: " + gameMap.getAllThrowable().size()
//                            + "\nArmor List Size: " + gameMap.getListArmors().size()
//                            + "\nHealth List Size: " + gameMap.getListHealingItems().size());

        //System.out.println("Other player : " + otherPlayers);

        currentTarget = FindNearestEnemy();
        if (currentTarget == null || !currentTarget.getIsAlive()) {
            currentTarget = FindNearestEnemy();
        }

        System.out.println(currentTarget);

        if (currentState != State.Attack) {

            // Prioritize finding weapons (guns, melee, throwable) first
            if (inventory.getGun() == null && !gameMap.getAllGun().isEmpty()) {
                currentState = State.FindGun;
            } else if (inventory.getMelee() != null && inventory.getMelee().getId().equals("HAND") && !gameMap.getAllMelee().isEmpty()) {
                currentState = State.FindMelee;
            } else if (inventory.getThrowable() == null && !gameMap.getAllThrowable().isEmpty()) {
                currentState = State.FindThrowable;
            }
//            else if (!gameMap.getListChests().isEmpty()) {
//                currentState = State.FindChest;  // After finding weapons, prioritize chests to get additional loot
//            }

            // After weapons and chests are found, check for health and armor
            else if (inventory.getListArmor().isEmpty() && !gameMap.getListArmors().isEmpty()) {
                currentState = State.FindArmor;
            }
//            else if ((inventory.getListHealingItem().isEmpty() && player.getHp() < 40) && !gameMap.getListHealingItems().isEmpty()) {
//                currentState = State.FindHealth;
//            }

            // If everything is collected, go after enemies
            else if (currentTarget != null && currentTarget.getIsAlive()) {
                currentState = State.FindEnemy;
            } else {
                currentState = State.FindEnemy;  // Default to finding the nearest enemy if no items need looting
            }
        }



        // Perform the action based on the current state
        Action(inventory);
    }

    private void Action(Inventory inventory) throws IOException {
        DoState(inventory);
    }

    private void DoState(Inventory inventory) throws IOException {
        switch(currentState){
            case FindWeapon:
            case FindHealth:
            case FindArmor:
            case FindGun:
            case FindThrowable:
            case FindMelee:
                hero.move(GetItemPath());
                if(SamePosition(currentPosition, currentItemTarget)) {
                    hero.pickupItem();
                }
                break;
            case FindChest:
            case FindEnemy:
                // Move towards the enemy
                hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, currentTarget, true));
                if (inventory.getGun() != null && MathHandler.Distance(currentPosition, currentTarget) <= inventory.getGun().getRange()) {
                    currentState = State.Attack;
                } else if (inventory.getMelee() != null && MathHandler.Distance(currentPosition, currentTarget) <= inventory.getMelee().getRange()) {
                    currentState = State.Attack;
                } else if (inventory.getThrowable() != null && MathHandler.Distance(currentPosition, currentTarget) <= inventory.getThrowable().getRange()) {
                    currentState = State.Attack;
                }
                break;
            case Attack:
                PerformAttack();  // Attack the enemy if in range
                break;
            case NeedHealing:
                Heal();  // Heal if necessary
                break;
        }
    }

    private void PerformAttack() throws IOException {
        // Calculate the distance between the bot and the current target
        double distanceToEnemy = MathHandler.Distance(currentPosition, currentTarget);

        System.out.println("Perform Attack");

        if(GetDirection().isEmpty()) hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, currentTarget, true));


        // If the bot has a melee weapon and the enemy is within melee range
        if (hero.getInventory().getMelee() != null && distanceToEnemy <= hero.getInventory().getMelee().getRange()) {
            hero.attack(GetDirection());
            return;
        }

        // If the bot has a gun and the enemy is within gun range
        if (hero.getInventory().getGun() != null && distanceToEnemy <= hero.getInventory().getGun().getRange()) {
            if (hero.getInventory().getGun().getCapacity() > 0) {
                System.out.println("Shoot");
                hero.shoot(GetDirection());
            } else {
                // If no bullets, remove the gun and switch to another weapon
                System.out.println("Throwgun");
                hero.revokeItem(hero.getInventory().getGun().getId());
            }
            return;
        }

        // If the bot has a throwable and the enemy is within throwable range
        if (hero.getInventory().getThrowable() != null && distanceToEnemy <= hero.getInventory().getThrowable().getRange()) {
            hero.throwItem(GetDirection());
            return;
        }

        // If no weapons are available, decide next action
        if (hero.getInventory().getGun() == null && hero.getInventory().getMelee() == null && hero.getInventory().getThrowable() == null) {
            // If no weapons are available, find the nearest weapon
            currentState = State.FindWeapon;  // Switch to FindWeapon state
            return;
        }

        System.out.println(currentTarget);

        // If enemy is out of range for any attack, move closer to the enemy
        hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, currentTarget, true));

        // After moving, recheck if the bot is now in attack range for the next cycle
        if (MathHandler.Distance(currentPosition, currentTarget) <= hero.getInventory().getGun().getRange() && currentTarget.getIsAlive()) {
            currentState = State.Attack;
        } else {
            currentState = null;
        }
    }

    private void BreakObstacle(Obstacle b) throws IOException {
        // Calculate the distance between the bot and the current target
        double distanceToEnemy = MathHandler.Distance(currentPosition, b);

        System.out.println("Perform Attack");

        if(GetDirection().isEmpty()) hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, b, true));


        // If the bot has a melee weapon and the enemy is within melee range
        if (hero.getInventory().getMelee() != null && distanceToEnemy <= hero.getInventory().getMelee().getRange()) {
            hero.attack(GetDirection());
            return;
        }

        // If the bot has a gun and the enemy is within gun range
        if (hero.getInventory().getGun() != null && distanceToEnemy <= hero.getInventory().getGun().getRange()) {
            if (hero.getInventory().getGun().getCapacity() > 0) {
                System.out.println("Shoot");
                hero.shoot(GetDirection());
            } else {
                // If no bullets, remove the gun and switch to another weapon
                System.out.println("Throwgun");
                hero.revokeItem(hero.getInventory().getGun().getId());
            }
            return;
        }

        // If the bot has a throwable and the enemy is within throwable range
        if (hero.getInventory().getThrowable() != null && distanceToEnemy <= hero.getInventory().getThrowable().getRange()) {
            hero.throwItem(GetDirection());
            return;
        }

        // If enemy is out of range for any attack, move closer to the enemy
        hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, b, true));

        // After moving, recheck if the bot is now in attack range for the next cycle
        if (MathHandler.Distance(currentPosition, b) <= hero.getInventory().getGun().getRange()) {
            currentState = State.BreakObstacle;
        } else {
            currentState = null;
        }
    }



    private void Heal() throws IOException {
        if(!hero.getInventory().getListHealingItem().isEmpty()){
            hero.useItem(hero.getInventory()
                    .getListHealingItem()
                    .getFirst()
                    .getId()
            );
            return;
        }

        currentState = State.FindHealth;
    }
//endregion

//region Find Nearest Algo
    private Node FindNearest(ItemType type) {
        return switch (type) {
            case ItemType.HEALTH -> FindNearestHealthPosition();
            case ItemType.WEAPON -> FindNearestWeapon();
            case ItemType.ARMOR -> FindNearestShieldPosition();
            case ItemType.CHEST -> FindNearestChestPosition();
            case ItemType.MELEE_WEAPON -> FindNearestMeleePosition();
            case ItemType.THROWABLE_WEAPON -> FindNearestThrowablePosition();
            case ItemType.GUN -> FindNearestGunPosition();
            default -> null;
        };
    }

    private Player FindNearestEnemy() {
        Player res = null;
        double distance;
        double dis = 1000000;

        //System.out.println("Find enemy: " + otherPlayers);

        for (Player p : otherPlayers) {
            distance = MathHandler.Distance(currentPosition, p);
            //System.out.println(p + " " + distance);
            //System.out.println(MathHandler.IsInSafeArea(p));
            if (dis > distance && MathHandler.IsInSafeArea(p) && p.getIsAlive()) {
                res = p;
                dis = distance;

                //System.out.println("asdfgasdg" + res);
            }
        }

        return res;
    }

    private Node FindNearestHealthPosition() {
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<HealingItem> array = gameMap.getListHealingItems();

        for (HealingItem p : array) {
            distance = MathHandler.Distance(currentPosition, p);
            if (dis > distance && MathHandler.IsInSafeArea(p)) {
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestShieldPosition() {
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Armor> array = gameMap.getListArmors();

        for (Armor p : array) {
            distance = MathHandler.Distance(currentPosition, p);
            if (dis > distance && MathHandler.IsInSafeArea(p)) {
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestChestPosition() {
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Obstacle> array = gameMap.getListChests();

        for (Obstacle p : array) {
            distance = MathHandler.Distance(currentPosition, p);
            if (dis > distance && MathHandler.IsInSafeArea(p)) {
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestMeleePosition() {
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Weapon> array = gameMap.getAllMelee();

        for (Weapon p : array) {
            distance = MathHandler.Distance(currentPosition, p);
            if (dis > distance && MathHandler.IsInSafeArea(p)) {
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestGunPosition() {
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Weapon> array = gameMap.getAllGun();

        for (Weapon p : array) {
            distance = MathHandler.Distance(currentPosition, p);
            if (dis > distance && MathHandler.IsInSafeArea(p)) {
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestThrowablePosition() {
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Weapon> array = gameMap.getAllThrowable();

        for (Weapon p : array) {
            distance = MathHandler.Distance(currentPosition, p);
            if (dis > distance && MathHandler.IsInSafeArea(p)) {
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestWeapon() {
        Node res = null;
        Node nearestMelee = FindNearestMeleePosition();
        Node nearestThrowable = FindNearestThrowablePosition();
        Node nearestGun = FindNearestGunPosition();

        // Start by comparing the non-null nearestMelee and nearestGun
        if (nearestMelee != null && nearestGun != null) {
            res = (MathHandler.Distance(currentPosition, nearestGun) >= MathHandler.Distance(currentPosition, nearestMelee))
                    ? nearestMelee : nearestGun;
        } else if (nearestMelee != null) {
            res = nearestMelee;
        } else if (nearestGun != null) {
            res = nearestGun;
        }

        // Now compare the result with nearestThrowable if it's not null
        if (nearestThrowable != null) {
            if (res == null || MathHandler.Distance(currentPosition, nearestThrowable) < MathHandler.Distance(currentPosition, res)) {
                res = nearestThrowable;
            }
        }

        System.out.println(res);

        return res;
    }

    private Node GetNearestTarget() {
        Node nearestChest = FindNearestChest();

        if (nearestChest != null) {
            // Compare distances and return the nearest target (either the current target or the nearest chest)
            return (MathHandler.Distance(currentPosition, currentTarget) <= MathHandler.Distance(currentPosition, nearestChest))
                    ? currentTarget
                    : nearestChest;
        }

        // If no chest is found, return the current target as the default
        return currentTarget;
    }

    private Node FindNearestChest(){
        if(gameMap.getListChests().isEmpty()) return null;

        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Obstacle> array = gameMap.getListChests();

        for (Obstacle p : array) {
            distance = MathHandler.Distance(currentPosition, p);
            if (dis > distance && MathHandler.IsInSafeArea(p)) {
                res = p;
                dis = distance;
            }
        }

        return res;
    }

//endregion

}
