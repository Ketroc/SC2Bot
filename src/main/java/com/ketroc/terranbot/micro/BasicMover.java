package com.ketroc.terranbot.micro;

import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Buffs;
import com.github.ocraft.s2client.protocol.data.UnitTypeData;
import com.github.ocraft.s2client.protocol.data.Weapon;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;
import com.ketroc.terranbot.utils.InfluenceMaps;
import com.ketroc.terranbot.utils.Position;
import com.ketroc.terranbot.utils.Time;
import com.ketroc.terranbot.utils.UnitUtils;
import com.ketroc.terranbot.bots.Bot;
import com.ketroc.terranbot.models.Ignored;
import com.ketroc.terranbot.models.IgnoredUnit;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class BasicMover {
    public UnitInPool mover;
    public Point2d targetPos;
    public boolean isGround;
    public boolean canAttackAir;
    public boolean canAttackGround;
    public float groundAttackRange;
    public float airAttackRange;
    public float movementSpeed;
    public int[][] threatMap;
    public boolean isDodgeClockwise;
    private long prevDirectionChangeFrame;

    public BasicMover(UnitInPool mover, Point2d targetPos) {
        this.mover = mover;
        this.targetPos = targetPos;
        this.isGround = !mover.unit().getFlying().orElse(false);
        setWeaponInfo();
        this.movementSpeed = Bot.OBS.getUnitTypeData(false).get(mover.unit().getType()).getMovementSpeed().orElse(0f);
        Ignored.add(new IgnoredUnit(mover.getTag()));
    }

    public void onStep() {
        threatMap = (isGround) ? InfluenceMaps.pointThreatToGround : InfluenceMaps.pointThreatToAir;

        //attack if available
        if (isOffCooldown()) {
            UnitInPool attackTarget = selectTarget();
            //attack if there's a target
            if (attackTarget != null) {
                if (!isTargettingUnit(attackTarget.unit())) {
                    Bot.ACTION.unitCommand(mover.unit(), Abilities.ATTACK, attackTarget.unit(), false);
                }
                return;
            }
        }
        //continue moving to target
        if (isSafe()) {
            if (!isMovingToTargetPos()) {
                Bot.ACTION.unitCommand(mover.unit(), Abilities.MOVE, targetPos, false);
            }
        }
        //detour if needed
        else {
            Point2d detourPos = findDetourPos();
            Bot.ACTION.unitCommand(mover.unit(), Abilities.MOVE, detourPos, false);
        }
    }

    private boolean isMovingToTargetPos() {
        return !mover.unit().getOrders().isEmpty() &&
                mover.unit().getOrders().get(0).getTargetedWorldSpacePosition().isPresent() &&
                mover.unit().getOrders().get(0).getTargetedWorldSpacePosition().get().toPoint2d().distance(targetPos) < 1;

    }

    private boolean isTargettingUnit(Unit target) {
        return !mover.unit().getOrders().isEmpty() &&
                target.getTag().equals(mover.unit().getOrders().get(0).getTargetedUnitTag().orElse(null));
    }

    //selects target based on cost:health ratio
    public UnitInPool selectTarget() {
        List<UnitInPool> enemiesInRange = Bot.OBS.getUnits(Alliance.ENEMY, enemy ->
                ((isAir(enemy) && canAttackAir) || (!isAir(enemy) && canAttackGround)) &&
                UnitUtils.getDistance(enemy.unit(), mover.unit()) <= (isAir(enemy) ? airAttackRange : groundAttackRange) &&
                !UnitUtils.IGNORED_TARGETS.contains(enemy.unit().getType()));
        Target bestTarget = new Target(null, Float.MAX_VALUE, Float.MAX_VALUE); //best target will be lowest hp unit without barrier
        for (UnitInPool enemy : enemiesInRange) {
            float enemyHP = enemy.unit().getHealth().orElse(0f) + enemy.unit().getShield().orElse(0f);
            UnitTypeData enemyData = Bot.OBS.getUnitTypeData(false).get(enemy.unit().getType());
            float enemyCost;
            if (enemy.unit().getType() == UnitUtils.enemyWorkerType) { //inflate value of workers as they impact income
                enemyCost = 75;
            }
            else {
                enemyCost = enemyData.getMineralCost().orElse(1) + (enemyData.getVespeneCost().orElse(1) * 1.2f); //value gas more than minerals
            }
            float enemyValue = enemyHP/enemyCost;
            if (enemyValue < bestTarget.value && !enemy.unit().getBuffs().contains(Buffs.PROTECTIVE_BARRIER)) {
                bestTarget.update(enemy, enemyValue, enemyHP);
            }
        }
        return bestTarget.unit;
    }

    private boolean isAir(UnitInPool enemy) {
        return enemy.unit().getFlying().orElse(true);
    }

    private List<UnitInPool> getValidTargetsInRange() {
        Predicate<UnitInPool> enemyTargetPredicate = enemy -> {
            boolean isEnemyGround = !enemy.unit().getFlying().orElse(false);
            float range = ((isEnemyGround) ? groundAttackRange : airAttackRange) + 1.5f;
            return ((isEnemyGround && canAttackGround) || (!isEnemyGround && canAttackAir)) &&
                    UnitUtils.getDistance(mover.unit(), targetPos) < range;
        };
        return Bot.OBS.getUnits(Alliance.ENEMY, enemyTargetPredicate);
    }

    private boolean isOffCooldown() {
        return mover.unit().getWeaponCooldown().orElse(1f) == 0;
    }

    private void setWeaponInfo() {
        Set<Weapon> weapons = Bot.OBS.getUnitTypeData(false).get(mover.unit().getType()).getWeapons();
        for (Weapon weapon : weapons) {
            switch (weapon.getTargetType()) {
                case AIR:
                    canAttackAir = true;
                    airAttackRange = weapon.getRange() + mover.unit().getRadius();
                    break;
                case GROUND:
                    canAttackGround = true;
                    groundAttackRange = weapon.getRange() + mover.unit().getRadius();
                    break;
                case ANY:
                    canAttackAir = true;
                    canAttackGround = true;
                    airAttackRange = weapon.getRange() + mover.unit().getRadius();
                    groundAttackRange = weapon.getRange() + mover.unit().getRadius();
                    break;
            }
        }
    }

    private boolean isSafe() {
        return isSafe(mover.unit().getPosition().toPoint2d());
    }

    private boolean isSafe(Point2d pos) {
        return InfluenceMaps.getValue(threatMap, pos) == 0;
    }

    private Point2d findDetourPos() {
        return findDetourPos(3.5f);
    }

    private Point2d findDetourPos(float rangeCheck) {
        Point2d towardsTarget = Position.towards(mover.unit().getPosition().toPoint2d(), targetPos, rangeCheck);
        Point2d safestPos = null;
        int safestThreatValue = Integer.MAX_VALUE;
        for (int i=0; i<360; i+=20) {
            int angle = (isDodgeClockwise) ? i : (i * -1);
            Point2d detourPos = Position.rotate(towardsTarget, mover.unit().getPosition().toPoint2d(), angle, true);
            if (detourPos == null || !isPathable(detourPos)) {
                continue;
            }
            int threatValue = InfluenceMaps.getValue(threatMap, detourPos);
            if (rangeCheck > 7 && threatValue < safestThreatValue) { //save least dangerous position in case no safe position is found
                safestThreatValue = threatValue;
                safestPos = detourPos;
            }
            if (isSafe(detourPos)) {
                if (i > 200 && !changedDirectionRecently()) { //Position.atEdgeOfMap(detourPos) ||
                    toggleDodgeClockwise();
                }
                //add 20degrees more angle as buffer, to account for chasing units
                i += 20;
                angle = (isDodgeClockwise) ? i : (i * -1);
                detourPos = Position.rotate(towardsTarget, mover.unit().getPosition().toPoint2d(), angle);
                return detourPos;
            }
        }
        if (safestPos == null) {
            return findDetourPos(rangeCheck+2);
        }
        else {
            return safestPos;
        }
    }

    private boolean isPathable(Point2d detourPos) {
        return !isGround || Bot.OBS.isPathable(detourPos);
    }

    public void toggleDodgeClockwise() {
        isDodgeClockwise = !isDodgeClockwise;
        prevDirectionChangeFrame = Time.nowFrames();
    }

    //3sec delay between direction changes (so it doesn't get stuck wiggling against the edge)
    public boolean changedDirectionRecently() {
        return prevDirectionChangeFrame + 75 > Time.nowFrames();
    }
}
