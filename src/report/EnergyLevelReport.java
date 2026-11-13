/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;


import core.DTNHost;
import core.SimError;
import core.UpdateListener;
import routing.ActiveRouter;
import routing.util.EnergyModel;

/**
 * Node energy level report. Reports the energy level of all 
 * (or only some, see {@link #REPORTED_NODES}) nodes every 
 * configurable-amount-of seconds (see {@link #GRANULARITY}).
 * Works only if all nodes use energy model; see 
 * {@link routing.util.EnergyModel}.
 */
public class EnergyLevelReport extends SnapshotReport 
	implements UpdateListener {

	@Override
	protected void writeSnapshot(DTNHost h) {
		if (!(h.getRouter() instanceof ActiveRouter)) {
			throw new SimError("Host " + h + " is not using ActiveRouter energy model");
		}

		EnergyModel model = ((ActiveRouter) h.getRouter()).getEnergyModel();
		if (model == null) {
			throw new SimError("Host " + h + " is not using energy model");
		}

		Double value = model.getEnergy();
		write(h.toString() + " remaining=" + format(value)
				+ " consumed=" + format(model.getConsumedEnergy())
				+ " initial=" + format(model.getInitialEnergy())
				+ " staticCacheLookup=" + format(model.getStaticCacheLookupEnergy())
				+ " staticCacheWrite=" + format(model.getStaticCacheWriteEnergy())
				+ " staticCacheInitWrite=" + format(model.getStaticCacheInitWriteEnergy())
				+ " packetTx=" + format(model.getPacketTransmissionEnergy())
				+ " packetRx=" + format(model.getPacketReceptionEnergy())
				+ " oppCacheLookup=" + format(model.getOppCacheLookupEnergy())
				+ " oppCacheWrite=" + format(model.getOppCacheWriteEnergy())
				+ " pitLookup=" + format(model.getPitLookupEnergy())
				+ " pitInsert=" + format(model.getPitInsertEnergy())
				+ " pitUpdate=" + format(model.getPitUpdateEnergy())
				+ " pitRemove=" + format(model.getPitRemoveEnergy()));
	}

}
