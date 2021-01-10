package com.ketroc.terranbot.micro;

import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Unit;
import com.ketroc.terranbot.bots.Bot;
import com.ketroc.terranbot.utils.LocationConstants;
import com.ketroc.terranbot.utils.Placement;
import com.ketroc.terranbot.utils.Position;
import com.ketroc.terranbot.utils.UnitUtils;

import java.util.ArrayList;
import java.util.List;

public class StructureFloater extends BasicUnitMicro {

    public StructureFloater(Unit structure) {
        super(Bot.OBS.getUnit(structure.getTag()), Position.toNearestHalfPoint(structure.getPosition().toPoint2d()), MicroPriority.SURVIVAL);
    }

    @Override
    public void onArrival() {
        if (unit.unit().getFlying().orElse(true)) {
            if (UnitUtils.getOrder(unit.unit()) != Abilities.LAND) {
                Bot.ACTION.unitCommand(unit.unit(), Abilities.LAND, targetPos, false);
            }
        }
        else {
            removeMe = true;
        }
    }

    @Override
    public void onDeath() {
        super.onDeath();
        switch ((Units)unit.unit().getType()) {
            case TERRAN_ORBITAL_COMMAND_FLYING: case TERRAN_COMMAND_CENTER_FLYING:
                Placement.possibleCcPosList.add(targetPos);
                break;
            case TERRAN_BARRACKS_FLYING:
                LocationConstants._3x3Structures.add(targetPos);
                break;
            case TERRAN_STARPORT_FLYING:
                LocationConstants.STARPORTS.add(targetPos);
                break;
        }
    }
}
