package org.openlca.core.database.upgrades;

import org.junit.Assert;
import org.junit.Test;
import org.openlca.core.Tests;
import org.openlca.core.database.IDatabase;

public class UpgradeUtilTest {

	private IDatabase database = Tests.getDb();
	private Util util = new Util(database);

	@Test
	public void testCreateTable() throws Exception {
		Assert.assertFalse(util.tableExists("upgrade_test_table"));
		String tableDef = "create table upgrade_test_table ("
				+ "id BIGINT NOT NULL, "
				+ "ref_id VARCHAR(36))";
		util.createTable("upgrade_test_table", tableDef);
		Assert.assertTrue(util.tableExists("upgrade_test_table"));
		Assert.assertTrue(util.columnExists("upgrade_test_table", "id"));
		util.dropTable("upgrade_test_table");
		Assert.assertFalse(util.tableExists("upgrade_test_table"));
	}

	@Test
	public void testAddColumn() throws Exception {
		String tableDef = "create table upgrade_test_table ("
				+ "id BIGINT NOT NULL, "
				+ "ref_id VARCHAR(36))";
		util.createTable("upgrade_test_table", tableDef);
		Assert.assertFalse(util.columnExists("upgrade_test_table", "test_col"));
		String colDef = "test_col VARCHAR(255)";
		util.createColumn("upgrade_test_table", "test_col", colDef);
		Assert.assertTrue(util.columnExists("upgrade_test_table", "test_col"));
		util.dropTable("upgrade_test_table");
		Assert.assertFalse(util.tableExists("upgrade_test_table"));
	}
}
