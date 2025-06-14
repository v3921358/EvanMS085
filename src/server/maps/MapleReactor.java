package server.maps;

import java.awt.Rectangle;
import server.Timer.MapTimer;
import scripting.ReactorScriptManager;
import tools.MaplePacketCreator;
import client.MapleClient;
import tools.Pair;

public class MapleReactor extends AbstractMapleMapObject {

    private int rid;
    private MapleReactorStats stats;
    private byte state;
    private int delay;
    private MapleMap map;
    private String name;
    private boolean timerActive;
    private boolean alive;

    public MapleReactor(final MapleReactorStats stats, final int rid) {
        this.name = "";
        this.stats = stats;
        this.rid = rid;
        this.alive = true;
    }

    public final byte getFacingDirection() {
        return this.stats.getFacingDirection();
    }

    public void setTimerActive(final boolean active) {
        this.timerActive = active;
    }

    public boolean isTimerActive() {
        return this.timerActive;
    }

    public int getReactorId() {
        return this.rid;
    }

    public void setState(final byte state) {
        this.state = state;
    }

    public byte getState() {
        return this.state;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public void setAlive(final boolean alive) {
        this.alive = alive;
    }

    public void setDelay(final int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return this.delay;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.REACTOR;
    }

    public int getReactorType() {
        return this.stats.getType(this.state);
    }

    public void setMap(final MapleMap map) {
        this.map = map;
    }

    public MapleMap getMap() {
        return this.map;
    }

    public Pair<Integer, Integer> getReactItem() {
        return this.stats.getReactItem(this.state);
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        client.sendPacket(MaplePacketCreator.destroyReactor(this));
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        client.sendPacket(MaplePacketCreator.spawnReactor(this));
    }

    public void forceStartReactor(final MapleClient c) {
        ReactorScriptManager.getInstance().act(c, this);
    }

    public void forceHitReactor(final byte newState) {
        this.setState(newState);
        this.setTimerActive(false);
        this.map.broadcastMessage(MaplePacketCreator.triggerReactor(this, 0));
    }

    public void hitReactor(final MapleClient c) {
        this.hitReactor(0, (short) 0, c);
    }

    public void forceTrigger() {
        this.map.broadcastMessage(MaplePacketCreator.triggerReactor(this, 0));
    }

    public void delayedDestroyReactor(final long delay) {
        MapTimer.getInstance().schedule((Runnable) new Runnable() {
            @Override
            public void run() {
                map.destroyReactor(MapleReactor.this.getObjectId());
            }
        }, delay);
    }

    public void hitReactor(final int charPos, final short stance, final MapleClient c) {
        if (this.stats.getType(this.state) < 999 && this.stats.getType(this.state) != -1) {
            final byte oldState = this.state;
            if (this.stats.getType(this.state) != 2 || (charPos != 0 && charPos != 2)) {
                this.state = this.stats.getNextState(this.state);
                if (this.stats.getNextState(this.state) == -1 || this.stats.getType(this.state) == 999) {
                    if ((this.stats.getType(this.state) < 100 || this.stats.getType(this.state) == 999) && this.delay > 0) {
                        this.map.destroyReactor(this.getObjectId());
                    } else {
                        this.map.broadcastMessage(MaplePacketCreator.triggerReactor(this, (int) stance));
                    }
                    ReactorScriptManager.getInstance().act(c, this);
                } else {
                    boolean done = false;
                    this.map.broadcastMessage(MaplePacketCreator.triggerReactor(this, (int) stance));
                    if (this.state == this.stats.getNextState(this.state) || this.rid == 2618000 || this.rid == 2309000) {
                        ReactorScriptManager.getInstance().act(c, this);
                        done = true;
                    }
                    if (this.stats.getTimeOut(this.state) > 0) {
                        if (!done) {
                            ReactorScriptManager.getInstance().act(c, this);
                        }
                        this.scheduleSetState(this.state, oldState, (long) this.stats.getTimeOut(this.state));
                    }
                }
            }
        }
    }

    public Rectangle getArea() {
        final int height = this.stats.getBR().y - this.stats.getTL().y;
        final int width = this.stats.getBR().x - this.stats.getTL().x;
        final int origX = this.getPosition().x + this.stats.getTL().x;
        final int origY = this.getPosition().y + this.stats.getTL().y;
        return new Rectangle(origX, origY, width, height);
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Reactor " + this.getObjectId() + " of id " + this.rid + " at position " + this.getPosition().toString() + " state" + (int) this.state + " type " + this.stats.getType(this.state);
    }

    public void delayedHitReactor(final MapleClient c, final long delay) {
        MapTimer.getInstance().schedule((Runnable) new Runnable() {
            @Override
            public void run() {
                MapleReactor.this.hitReactor(c);
            }
        }, delay);
    }

    public void scheduleSetState(final byte oldState, final byte newState, final long delay) {
        MapTimer.getInstance().schedule((Runnable) new Runnable() {
            @Override
            public void run() {
                if (state == oldState) {
                    MapleReactor.this.forceHitReactor(newState);
                }
            }
        }, delay);
    }
}
