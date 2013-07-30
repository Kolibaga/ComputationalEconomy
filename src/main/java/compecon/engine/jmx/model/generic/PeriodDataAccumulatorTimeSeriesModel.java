/*
This file is part of ComputationalEconomy.

ComputationalEconomy is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ComputationalEconomy is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ComputationalEconomy. If not, see <http://www.gnu.org/licenses/>.
 */

package compecon.engine.jmx.model.generic;

import java.util.Map.Entry;

import org.jfree.data.time.Day;

import compecon.engine.time.TimeSystem;

public class PeriodDataAccumulatorTimeSeriesModel<T> extends
		PeriodDataAbstractTimeSeriesModel<T> {

	protected final PeriodDataAccumulatorSet<T> periodDataAccumulatorSet;

	public PeriodDataAccumulatorTimeSeriesModel(T[] initialTypes) {
		this.periodDataAccumulatorSet = new PeriodDataAccumulatorSet<T>(
				initialTypes);
		for (T type : initialTypes)
			assureTimeSeries(type);
	}

	public PeriodDataAccumulatorTimeSeriesModel(T[] initialTypes,
			String titleSuffix) {
		this(initialTypes);
		this.titleSuffix = titleSuffix;
	}

	public void add(T type, double amount) {
		if (this.timeSeries.containsKey(type)) {
			this.periodDataAccumulatorSet.add(type, amount);
		}
	}

	public void nextPeriod() {
		// write amount for each type into corresponding time series
		for (Entry<T, PeriodDataAccumulator> entry : this.periodDataAccumulatorSet
				.getPeriodDataAccumulators().entrySet()) {
			if (this.timeSeries.containsKey(entry.getKey())) {
				this.timeSeries.get(entry.getKey()).addOrUpdate(
						new Day(TimeSystem.getInstance().getCurrentDate()),
						entry.getValue().getAmount());
			}
		}

		this.periodDataAccumulatorSet.reset();
	}
}
