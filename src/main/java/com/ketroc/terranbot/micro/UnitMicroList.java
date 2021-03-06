package com.ketroc.terranbot.micro;

import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.unit.Tag;
import com.ketroc.terranbot.models.Ignored;
import com.ketroc.terranbot.models.IgnoredUnit;
import com.ketroc.terranbot.utils.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UnitMicroList {
    public static List<BasicUnitMicro> unitMicroList = new ArrayList<>();

    public static void onStep() {
        long microStartTime = System.currentTimeMillis();
        unitMicroList.forEach(BasicUnitMicro::onStep);
        if (Time.nowFrames() % 1344 == 0) { //once a minute
            System.out.println("num cyclones micro'ing = " + UnitMicroList.getUnitSubList(Cyclone.class).size());
            System.out.println("num units micro'ing = " + unitMicroList.size());
            System.out.println("time taken (ms) = " + (System.currentTimeMillis() - microStartTime));
        }

        List<Tag> removeList = unitMicroList.stream()
                .filter(basicUnitMicro -> basicUnitMicro.removeMe)
                .map(basicUnitMicro -> basicUnitMicro.unit.getTag())
                .collect(Collectors.toList());
        removeList.forEach(tag -> UnitMicroList.remove(tag));
    }

    public static void add(BasicUnitMicro microUnit) {
        Ignored.add(new IgnoredUnit(microUnit.unit.getTag()));
        unitMicroList.add(microUnit);
    }

    public static void remove(Tag microUnitTag) {
        Ignored.remove(microUnitTag);
        for (int i=0; i<unitMicroList.size(); i++) { //remove first entry only
            if (unitMicroList.get(i).unit.getTag().equals(microUnitTag)) {
                unitMicroList.remove(i);
                return;
            }
        }
    }

    public static <T extends BasicUnitMicro> List<T> getUnitSubList(Class<T> cls) {
        return unitMicroList.stream()
                .filter(basicUnit -> cls.isInstance(basicUnit))
                .map(basicUnit -> cls.cast(basicUnit))
                .collect(Collectors.toList());
    }
}
