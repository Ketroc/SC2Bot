package com.ketroc.terranbot.purchases;

import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Units;
import com.ketroc.terranbot.Bot;
import com.ketroc.terranbot.models.Cost;
import com.ketroc.terranbot.GameState;

public class PurchaseUnit implements Purchase {
    private Cost cost;
    private Units unitType;
    private UnitInPool productionStructure;

    public PurchaseUnit(Units unitType, UnitInPool productionStructure) {
        this.unitType = unitType;
        this.productionStructure = productionStructure;
        setCost();
    }

    @Override
    public Cost getCost() {
        return cost;
    }

    public void setCost(Cost cost) {
        this.cost = cost;
    }

    public Units getUnitType() {
        return unitType;
    }

    public void setUnitType(Units unitType) {
        this.unitType = unitType;
    }

    public UnitInPool getProductionStructure() {
        return productionStructure;
    }

    public void setProductionStructure(UnitInPool productionStructure) {
        this.productionStructure = productionStructure;
    }

    @Override
    public PurchaseResult build() {
        if (!productionStructure.isAlive()) {
            return PurchaseResult.CANCEL;
        }
        if (canAfford()) {
            Bot.ACTION.unitCommand(productionStructure.unit(), Bot.OBS.getUnitTypeData(false).get(unitType).getAbility().get(), false);
            Cost.updateBank(cost);
            return PurchaseResult.SUCCESS;
        }
        Cost.updateBank(cost);
        return PurchaseResult.WAITING;
    }

    @Override
    public boolean canAfford() {
        return GameState.mineralBank >= cost.minerals && GameState.gasBank >= cost.gas;
    }

    @Override
    public void setCost() {
        cost = Cost.getUnitCost(unitType);
    }

    @Override
    public String getType() {
        return unitType.toString();
    }
}
