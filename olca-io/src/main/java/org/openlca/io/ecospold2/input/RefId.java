package org.openlca.io.ecospold2.input;

import org.openlca.ecospold2.DataSet;
import org.openlca.ecospold2.IntermediateExchange;
import org.openlca.util.KeyGen;

/**
 * Generates the reference IDs for EcoSpold 02 entities.
 */
final class RefId {

	private RefId() {
	}

	/**
	 * Returns the combination of activity-ID and reference product-ID (in this
	 * order) as UUID.
	 */
	public static String forProcess(DataSet dataSet) {
		if (dataSet.getActivity() == null)
			return KeyGen.NULL_UUID;
		String productId = null;
		for (IntermediateExchange exchange : dataSet.getIntermediateExchanges()) {
			if (exchange.outputGroup == null)
				continue;
			if (exchange.outputGroup == 0 && exchange.amount != 0) {
				productId = exchange.intermediateExchangeId;
				break;
			}
		}
		return KeyGen.get(dataSet.getActivity().getId(), productId);
	}

}
