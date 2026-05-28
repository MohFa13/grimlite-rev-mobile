package com.grimliterev.overlay.game;

public interface GameInteractor {
    void tap(int x, int y);
    void swipe(int x1, int y1, int x2, int y2, int durationMs);
    void longPress(int x, int y, int durationMs);
    void kill(String monster);
    void join(String map);
    void acceptQuest(String quest);
    void turnInQuest(String quest);
    void rest();
    void useSkill(int slot);
    void move(int x, int y);
    void buy(String item);
    void sell(String item);
    void equip(String item);
    void unequip(String item);
    boolean getState(String key);
}
