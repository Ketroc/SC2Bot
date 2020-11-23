package com.ketroc.terranbot.bots;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.*;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.data.Upgrades;
import com.ketroc.terranbot.managers.ActionErrorManager;

import java.util.HashMap;
import java.util.Map;

public class Bot extends S2Agent {
    public static ActionInterface ACTION;
    public static ObservationInterface OBS;
    public static QueryInterface QUERY;
    public static DebugInterface DEBUG;
    public static ControlInterface CONTROL;
    public static boolean isDebugOn;
    public static boolean isRealTime;
    public static String opponentId;
    public static Map<Abilities, Units> abilityToUnitType = new HashMap<>(); //TODO: move
    public static Map<Abilities, Upgrades> abilityToUpgrade = new HashMap<>(); //TODO: move

    public Bot(boolean isDebugOn, String opponentId, boolean isRealTime) {
        this.isDebugOn = isDebugOn;
        this.opponentId = opponentId;
        this.isRealTime = isRealTime;
    }

    @Override
    public void onGameStart() {
        OBS = observation();
        ACTION = actions();
        QUERY = query();
        DEBUG = debug();
        CONTROL = control();

        //load abilityToUnitType map
        Bot.OBS.getUnitTypeData(false).forEach((unitType, unitTypeData) -> {
            unitTypeData.getAbility().ifPresent(ability -> {
                if (ability instanceof Abilities && unitType instanceof Units) {
                    Bot.abilityToUnitType.put((Abilities) ability, (Units) unitType);
                }
            });
        });

        //load abilityToUpgrade map
        Bot.OBS.getUpgradeData(false).forEach((upgrade, upgradeData) -> {
            upgradeData.getAbility().ifPresent(ability -> {
                if (ability instanceof Abilities && upgrade instanceof Upgrades) {
                    switch ((Abilities) ability) { //fix for api bug
                        case RESEARCH_TERRAN_VEHICLE_AND_SHIP_PLATING_LEVEL1_V2:
                            ability = Abilities.RESEARCH_TERRAN_VEHICLE_AND_SHIP_PLATING_LEVEL1;
                            break;
                        case RESEARCH_TERRAN_VEHICLE_AND_SHIP_PLATING_LEVEL2_V2:
                            ability = Abilities.RESEARCH_TERRAN_VEHICLE_AND_SHIP_PLATING_LEVEL2;
                            break;
                        case RESEARCH_TERRAN_VEHICLE_AND_SHIP_PLATING_LEVEL3_V2:
                            ability = Abilities.RESEARCH_TERRAN_VEHICLE_AND_SHIP_PLATING_LEVEL3;
                            break;
                    }
                    Bot.abilityToUpgrade.put((Abilities) ability, (Upgrades) upgrade);
                }
            });
        });
    }

    @Override
    public void onStep() {
        Bot.OBS.getActionErrors().forEach(actionError -> ActionErrorManager.actionErrorList.add(actionError));

    }
}
