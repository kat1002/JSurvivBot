import io.socket.emitter.Emitter;
import jsclub.codefest2024.sdk.Hero;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;
import jsclub.codefest2024.sdk.model.Inventory;
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
    public Node currentItemTarget;
    public Node currentPosition;


    public GameMap gameMap;
    public List<Player> otherPlayers;
    public List<Obstacle> restricedList;
    public List<Node> restrictedNodes;
    public List<Node> otherPlayesPosition;

    public Bot(String GameId, String PlayerName) {
        hero = new Hero(GameId, PlayerName);
        restrictedNodes = new ArrayList<>();
        otherPlayesPosition = new ArrayList<>();
    }

    public static Bot GetInstance() {
        if (Instance == null) {
            Instance = new Bot(BotInfo.GAME_ID, BotInfo.PLAYER_NAME);
        }
        return Instance;
    }

//region Method
    public void InitData() {
        player = gameMap.getCurrentPlayer();
        otherPlayers = gameMap.getOtherPlayerInfo();
        restricedList = gameMap.getListIndestructibleObstacles();
        restricedList.addAll(gameMap.getListChests());
        restricedList.addAll(gameMap.getListTraps());
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

    private String GetDirection(){
        if(currentPosition.getX() > currentTarget.getX()) return "r";
        if(currentPosition.getX() < currentTarget.getX()) return "l";
        if(currentPosition.getY() > currentTarget.getY()) return "d";
        if(currentPosition.getY() < currentTarget.getY()) return "u";

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

            System.out.println(gameMap.getCurrentPlayer());
            System.out.println(gameMap.getOtherPlayerInfo());

            RunLogic();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//region Bot Logic
    private void RunLogic() throws IOException {
        Inventory inventory = hero.getInventory();
        //if(currentBaseState.equals(BaseState.Prepare)) restrictedNodes.addAll(otherPlayesPosition);

        if(currentTarget != null && !currentTarget.getIsAlive())
            currentTarget = FindNearestEnemy();

        System.out.println(currentTarget);

//        System.out.println("Inventory: "
//                            + "\nGun: " + inventory.getGun() + " " + (inventory.getGun() == null  && gameMap.getAllGun().size() > 0)
//                            + "\nMelee: " + inventory.getMelee() + " " + (inventory.getMelee().getId().equals("HAND") && gameMap.getAllMelee().size() > 0)
//                            + "\nThrowable: " + inventory.getThrowable() + " " + (inventory.getThrowable() == null  && gameMap.getAllThrowable().size() > 0));

//        System.out.println("GameMap: "
//                            + "\nGun List Size: " + gameMap.getAllGun().size()
//                            + "\nMelee List Size: " + gameMap.getAllMelee().size()
//                            + "\nThrowable List Size: " + gameMap.getAllThrowable().size()
//                            + "\nArmor List Size: " + gameMap.getListArmors().size()
//                            + "\nHealth List Size: " + gameMap.getListHealingItems().size());

        if(inventory.getListArmor().size() <= 0 && gameMap.getListArmors().size() > 0) currentState = State.FindArmor;
        else if ((inventory.getListHealingItem().size() <= 0 || inventory.getListHealingItem().size() < 4) && gameMap.getListHealingItems().size() > 0) currentState = State.FindHealth;
        else if (inventory.getMelee().getId().equals("HAND") && gameMap.getAllMelee().size() > 0) currentState = State.FindMelee;
        else if (inventory.getGun() == null  && gameMap.getAllGun().size() > 0) currentState = State.FindGun;
        else if (inventory.getThrowable() == null && gameMap.getAllThrowable().size() > 0) currentState = State.FindThrowable;
        else if (inventory.getMelee().getId().equals("HAND") && gameMap.getAllMelee().size() > 0) currentState = State.FindMelee;
        else currentState = State.FindEnemy;

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
            case FindChest:
            case FindGun:
            case FindThrowable:
            case FindMelee:
                hero.move(GetItemPath());
                if(SamePosition(currentPosition, currentItemTarget))
                    hero.pickupItem();
                break;
            case FindEnemy:
                hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, currentTarget, true));
                break;
            case NeedHealing:
                Heal();
                break;
            case Attack:
                PerformAttack();
        }
    }

    private void PerformAttack() throws IOException {

        if(MathHandler.Distance(currentPosition, currentTarget) >= 2 && MathHandler.Distance(currentPosition, currentTarget) <= hero.getInventory().getGun().getRange()){
            if(hero.getInventory().getGun() != null){
                if(player.getBulletNum() > 0) hero.shoot(GetDirection());
                else hero.revokeItem(hero.getInventory().getGun().getId());
                return;
            }
            if(hero.getInventory().getThrowable() != null){
                hero.throwItem(GetDirection());
                return;
            }

            hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, currentTarget, true));
        }
        else{
            hero.attack(GetDirection());
        }
    }

    private void Heal() throws IOException {
        if(hero.getInventory().getListHealingItem().size() > 0){
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
        double dis = Double.MAX_VALUE;

        for (Player p : otherPlayers) {
            distance = MathHandler.Distance(currentPosition, p);
            System.out.println();
            if (dis > distance && MathHandler.IsInSafeArea(p)) {
                res = p;
                dis = distance;
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
            if (dis > distance || MathHandler.IsInSafeArea(p)) {
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
            if (dis > distance || MathHandler.IsInSafeArea(p)) {
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
            if (dis > distance || MathHandler.IsInSafeArea(p)) {
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
            if (dis > distance || MathHandler.IsInSafeArea(p)) {
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
            if (dis > distance || MathHandler.IsInSafeArea(p)) {
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
            if (dis > distance || MathHandler.IsInSafeArea(p)) {
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

        return res;
    }


//endregion

}
