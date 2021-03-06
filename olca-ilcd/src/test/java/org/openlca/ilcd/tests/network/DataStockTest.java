package org.openlca.ilcd.tests.network;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openlca.ilcd.descriptors.DataStock;
import org.openlca.ilcd.descriptors.DataStockList;
import org.openlca.ilcd.io.NetworkClient;

public class DataStockTest {

	@Test
	@Ignore
	public void testGetDataStocks() throws Exception {
		String url = "http://oekobaudat.online-now.de/OEKOBAU.DAT/resource";
		int numberOfStocks = 3;
		String shortName = "default";
		NetworkClient client = new NetworkClient(url);
		client.connect();
		DataStockList list = client.getDataStockList();
		Assert.assertEquals(numberOfStocks, list.dataStocks.size());
		boolean found = false;
		for (DataStock dataStock : list.dataStocks) {
			String sn = dataStock.shortName;
			if (shortName.equals(sn)) {
				found = true;
				break;
			}
		}
		Assert.assertTrue("Could not find data stock " + shortName, found);
		client.close();
	}

}
