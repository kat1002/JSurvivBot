import io.socket.emitter.Emitter;
import jsclub.codefest2024.sdk.Hero;
import jsclub.codefest2024.sdk.algorithm.PathUtils;
import jsclub.codefest2024.sdk.base.Node;
import jsclub.codefest2024.sdk.model.GameMap;
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
    public GameMap gameMap;
    public List<Player> otherPlayers;
    public List<Obstacle> restricedList;
    public List<Node> restrictedNodes;
    public List<Node> otherPlayesPosition;
    public Node currentPosition;

    State currentState;

    public static Bot GetInstance(){
        if(Instance == null) {
            Instance = new Bot(BotInfo.GAME_ID, BotInfo.PLAYER_NAME);
        }
        return Instance;
    }

    public Bot(String GameId, String PlayerName){
        hero = new Hero(GameId, PlayerName);
        restrictedNodes = new ArrayList<>();
        otherPlayesPosition = new ArrayList<>();
    }

    public void InitData(){
        player = gameMap.getCurrentPlayer();
        otherPlayers = gameMap.getOtherPlayerInfo();
        restricedList = gameMap.getListIndestructibleObstacles();
        restricedList.addAll(gameMap.getListChests());
        restricedList.addAll(gameMap.getListTraps());
        restrictedNodes.clear();
        otherPlayesPosition.clear();

        currentPosition = GetCurrentPosition();

        for (Player p : otherPlayers) {
            if(p.getIsAlive()){
                otherPlayesPosition.add(new Node(p.getX(), p.getY()));
            }
        }
        for (Obstacle o : restricedList) {
            restrictedNodes.add(new Node(o.getX(), o.getY()));
        }
    }

    public void Update(Object... args){
        try {
            System.out.println("Start");

            if(!player.getIsAlive()) return;

            gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);

            InitData();

            Weapon isUseGun = hero.getInventory().getGun();
            final boolean[] pickedUpGun = {isUseGun != null};

            System.out.println("inventory: " + isUseGun);
            System.out.println("is picked up: " + pickedUpGun[0]);

            if (!pickedUpGun[0]) {
                List<Weapon> gunList = gameMap.getAllGun();
                Weapon someGun = gunList.get(0);



                if (GetCurrentPosition().equals(new Node(someGun.getX(), someGun.getY()))) {
                    hero.pickupItem();
                    pickedUpGun[0] = true;
                } else {
                    restrictedNodes.addAll(otherPlayesPosition);
                    hero.move(PathUtils.getShortestPath(gameMap, restrictedNodes, currentPosition, someGun, false));
                }
            }

            System.out.println("End");

        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private Node GetCurrentPosition(){
        return new Node(player.getX(), player.getY());
    }

    private void PickUpItem() throws IOException {
        if(currentPosition.getX() == item.getX() && currentPosition.getY() == item.getY()){
            hero.pickupItem();
        }
    }

    private Node FindNearest(EntityType type){
        return switch (type) {
            case EntityType.ENEMY -> FindNearestEnemyPlayerPosition();
            case EntityType.HEALTH -> FindNearestHealthPosition();
            case EntityType.WEAPON -> FindNearestWeapon();
            case EntityType.SHIELD -> FindNearestShieldPosition();
            case EntityType.CHEST -> FindNearestChestPosition();
            default -> null;
        };
    }
//region Find Nearest Algo
    private Node FindNearestEnemyPlayerPosition(){
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;

        for(Node p : otherPlayesPosition){
            distance = MathHandler.Distance(currentPosition, p);
            if(dis > distance || MathHandler.IsInSafeArea(p)){
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestHealthPosition(){
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<HealingItem> array = gameMap.getListHealingItems();

        for(HealingItem p : array){
            distance = MathHandler.Distance(currentPosition, p);
            if(dis > distance || MathHandler.IsInSafeArea(p)){
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestShieldPosition(){
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Armor> array = gameMap.getListArmors();

        for(Armor p : array){
            distance = MathHandler.Distance(currentPosition, p);
            if(dis > distance || MathHandler.IsInSafeArea(p)){
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestChestPosition(){
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Obstacle> array = gameMap.getListChests();

        for(Obstacle p : array){
            distance = MathHandler.Distance(currentPosition, p);
            if(dis > distance || MathHandler.IsInSafeArea(p)){
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestMeleePosition(){
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Weapon> array = gameMap.getAllMelee();

        for(Weapon p : array){
            distance = MathHandler.Distance(currentPosition, p);
            if(dis > distance || MathHandler.IsInSafeArea(p)){
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestGunPosition(){
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Weapon> array = gameMap.getAllGun();

        for(Weapon p : array){
            distance = MathHandler.Distance(currentPosition, p);
            if(dis > distance || MathHandler.IsInSafeArea(p)){
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestThrowablePosition(){
        Node res = null;
        double distance;
        double dis = Double.MAX_VALUE;
        List<Weapon> array = gameMap.getAllThrowable();

        for(Weapon p : array){
            distance = MathHandler.Distance(currentPosition, p);
            if(dis > distance || MathHandler.IsInSafeArea(p)){
                res = p;
                dis = distance;
            }
        }

        return res;
    }

    private Node FindNearestWeapon(){
        Node res = null;
        Node nearestMelee = FindNearestMeleePosition();
        Node nearestThrowable = FindNearestThrowablePosition();
        Node nearestGun = FindNearestGunPosition();
        res = (MathHandler.Distance(currentPosition, nearestGun) >= MathHandler.Distance(currentPosition, nearestMelee)) ? nearestMelee : nearestGun;
        res = (MathHandler.Distance(currentPosition, nearestThrowable) >= MathHandler.Distance(currentPosition, res)) ? res : nearestThrowable;

        return res;
    }
//endregion

}
