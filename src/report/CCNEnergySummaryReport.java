/*
 * Copyright 2026 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.List;

import core.DTNHost;
import core.UpdateListener;
import routing.ActiveRouter;
import routing.util.EnergyModel;

/**
 * Aggregates the total energy usage of all hosts at the last snapshot point.
 * The result is a single summary line for the whole CCN network, suitable for
 * reporting total energy after the simulation.
 */
public class CCNEnergySummaryReport extends Report implements UpdateListener {
	private double totalRemainingEnergy;
	private double totalConsumedEnergy;
	private double totalInitialEnergy;
	private double totalStaticCacheLookupEnergy;
	private double totalStaticCacheWriteEnergy;
	private double totalStaticCacheInitWriteEnergy;
	private double totalPacketTransmissionEnergy;
	private double totalPacketReceptionEnergy;
	private double totalOppCacheLookupEnergy;
	private double totalOppCacheWriteEnergy;
	private double totalPitLookupEnergy;
	private double totalPitInsertEnergy;
	private double totalPitUpdateEnergy;
	private double totalPitRemoveEnergy;
	private int totalNodes;
	private boolean hasSample;

	public CCNEnergySummaryReport() {
		super();
		this.totalRemainingEnergy = 0.0;
		this.totalConsumedEnergy = 0.0;
		this.totalInitialEnergy = 0.0;
		this.totalStaticCacheLookupEnergy = 0.0;
		this.totalStaticCacheWriteEnergy = 0.0;
		this.totalStaticCacheInitWriteEnergy = 0.0;
		this.totalPacketTransmissionEnergy = 0.0;
		this.totalPacketReceptionEnergy = 0.0;
		this.totalOppCacheLookupEnergy = 0.0;
		this.totalOppCacheWriteEnergy = 0.0;
		this.totalPitLookupEnergy = 0.0;
		this.totalPitInsertEnergy = 0.0;
		this.totalPitUpdateEnergy = 0.0;
		this.totalPitRemoveEnergy = 0.0;
		this.totalNodes = 0;
		this.hasSample = false;
	}

	@Override
	public void updated(List<DTNHost> hosts) {
		this.totalRemainingEnergy = 0.0;
		this.totalConsumedEnergy = 0.0;
		this.totalInitialEnergy = 0.0;
		this.totalStaticCacheLookupEnergy = 0.0;
		this.totalStaticCacheWriteEnergy = 0.0;
		this.totalStaticCacheInitWriteEnergy = 0.0;
		this.totalPacketTransmissionEnergy = 0.0;
		this.totalPacketReceptionEnergy = 0.0;
		this.totalOppCacheLookupEnergy = 0.0;
		this.totalOppCacheWriteEnergy = 0.0;
		this.totalPitLookupEnergy = 0.0;
		this.totalPitInsertEnergy = 0.0;
		this.totalPitUpdateEnergy = 0.0;
		this.totalPitRemoveEnergy = 0.0;
		this.totalNodes = 0;
		this.hasSample = false;

		for (DTNHost h : hosts) {
			if (!(h.getRouter() instanceof ActiveRouter)) {
				continue;
			}
			EnergyModel model = ((ActiveRouter) h.getRouter()).getEnergyModel();
			if (model == null) {
				continue;
			}

			this.totalNodes++;
			this.totalRemainingEnergy += model.getEnergy();
			this.totalInitialEnergy += model.getInitialEnergy();
			this.totalConsumedEnergy += model.getConsumedEnergy();
			this.totalStaticCacheLookupEnergy += model.getTotalStaticCacheLookupEnergy();
			this.totalStaticCacheWriteEnergy += model.getTotalStaticCacheWriteEnergy();
			this.totalStaticCacheInitWriteEnergy += model.getTotalStaticCacheInitWriteEnergy();
			this.totalPacketTransmissionEnergy += model.getTotalPacketTransmissionEnergy();
			this.totalPacketReceptionEnergy += model.getTotalPacketReceptionEnergy();
			this.totalOppCacheLookupEnergy += model.getTotalOppCacheLookupEnergy();
			this.totalOppCacheWriteEnergy += model.getTotalOppCacheWriteEnergy();
			this.totalPitLookupEnergy += model.getTotalPitLookupEnergy();
			this.totalPitInsertEnergy += model.getTotalPitInsertEnergy();
			this.totalPitUpdateEnergy += model.getTotalPitUpdateEnergy();
			this.totalPitRemoveEnergy += model.getTotalPitRemoveEnergy();
		}

		this.hasSample = (this.totalNodes > 0);
	}

	@Override
	public void done() {
		if (this.hasSample) {
			write("initial_energy=" + format(this.totalInitialEnergy));
			write("remaining_energy=" + format(this.totalRemainingEnergy));
			write("total_consumed_energy=" + format(this.totalConsumedEnergy));
			write("static_cache_lookup_cost=" + format(this.totalStaticCacheLookupEnergy));
			write("static_cache_write_cost=" + format(this.totalStaticCacheWriteEnergy));
			write("static_cache_init_write_cost=" + format(this.totalStaticCacheInitWriteEnergy));
			write("packet_transmission_cost=" + format(this.totalPacketTransmissionEnergy));
			write("packet_reception_cost=" + format(this.totalPacketReceptionEnergy));
			write("oppo_cache_lookup_cost=" + format(this.totalOppCacheLookupEnergy));
			write("oppo_cache_write_cost=" + format(this.totalOppCacheWriteEnergy));
			write("pit_lookup_cost=" + format(this.totalPitLookupEnergy));
			write("pit_insert_cost=" + format(this.totalPitInsertEnergy));
			write("pit_update_cost=" + format(this.totalPitUpdateEnergy));
			write("pit_remove_cost=" + format(this.totalPitRemoveEnergy));
		}
		super.done();
	}
}
