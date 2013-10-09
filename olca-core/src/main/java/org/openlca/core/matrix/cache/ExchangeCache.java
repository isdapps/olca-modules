package org.openlca.core.matrix.cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.CalcExchange;
import org.openlca.core.model.UncertaintyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

class ExchangeCache {

	public static LoadingCache<Long, List<CalcExchange>> create(
			IDatabase database, ConversionTable conversionTable,
			FlowTypeTable flowTypes) {
		return CacheBuilder.newBuilder().build(
				new ExchangeLoader(database, conversionTable, flowTypes));
	}

	private static class ExchangeLoader extends
			CacheLoader<Long, List<CalcExchange>> {

		private Logger log = LoggerFactory.getLogger(getClass());
		private IDatabase database;
		private ConversionTable conversionTable;
		private FlowTypeTable flowTypes;

		public ExchangeLoader(IDatabase database,
				ConversionTable conversionTable, FlowTypeTable flowTypes) {
			this.database = database;
			this.conversionTable = conversionTable;
			this.flowTypes = flowTypes;
		}

		@Override
		public List<CalcExchange> load(Long key) throws Exception {
			if (key == null)
				return Collections.emptyList();
			log.trace("fetch exchanges for key {}", key);
			String query = "select * from tbl_exchanges where f_owner = " + key;
			try (Connection con = database.createConnection()) {
				Statement statement = con.createStatement();
				ResultSet result = statement.executeQuery(query);
				ArrayList<CalcExchange> exchanges = new ArrayList<>();
				while (result.next()) {
					CalcExchange exchange = nextExchange(result);
					exchanges.add(exchange);
				}
				result.close();
				statement.close();
				log.trace("fetched {} exchanges", exchanges.size());
				return exchanges;
			} catch (Exception e) {
				log.error("failed to fetch exchange vector", e);
				return Collections.emptyList();
			}
		}

		@Override
		public Map<Long, List<CalcExchange>> loadAll(
				Iterable<? extends Long> keys) throws Exception {
			log.trace("fetch exchanges for multiple keys");
			try (Connection con = database.createConnection()) {
				String query = "select * from tbl_exchanges where f_owner in "
						+ CacheUtil.asSql(keys);
				Statement statement = con.createStatement();
				HashMap<Long, List<CalcExchange>> map = new HashMap<>();
				ResultSet result = statement.executeQuery(query);
				while (result.next()) {
					CalcExchange exchange = nextExchange(result);
					CacheUtil.addListEntry(map, exchange,
							exchange.getProcessId());
				}
				result.close();
				statement.close();
				log.trace("{} lists loaded", map.size());
				return map;
			} catch (Exception e) {
				log.error("failed to fetch exchange vectors", e);
				return Collections.emptyMap();
			}
		}

		private CalcExchange nextExchange(ResultSet r) throws Exception {
			CalcExchange e = new CalcExchange();
			e.setProcessId(r.getLong("f_owner"));
			e.setAmount(r.getDouble("resulting_amount_value"));
			e.setAmountFormula(r.getString("resulting_amount_formula"));
			double factor = getConversionFactor(r);
			e.setConversionFactor(factor);
			e.setExchangeId(r.getLong("id"));
			e.setFlowId(r.getLong("f_flow"));
			e.setFlowType(flowTypes.getType(e.getFlowId()));
			e.setInput(r.getBoolean("is_input"));
			e.setDefaultProviderId(r.getLong("f_default_provider"));
			int uncertaintyType = r.getInt("distribution_type");
			if (!r.wasNull()) {
				e.setUncertaintyType(UncertaintyType.values()[uncertaintyType]);
				e.setParameter1(r.getDouble("parameter1_value"));
				e.setParameter2(r.getDouble("parameter2_value"));
				e.setParameter3(r.getDouble("parameter3_value"));
				e.setParameter1Formula(r.getString("parameter1_formula"));
				e.setParameter2Formula(r.getString("parameter2_formula"));
				e.setParameter3Formula(r.getString("parameter3_formula"));
			}
			return e;
		}

		private double getConversionFactor(ResultSet record) throws Exception {
			long propertyFactorId = record.getLong("f_flow_property_factor");
			double propertyFactor = conversionTable
					.getFlowPropertyFactor(propertyFactorId);
			long unitId = record.getLong("f_unit");
			double unitFactor = conversionTable.getUnitFactor(unitId);
			if (propertyFactor == 0)
				return 0;
			return unitFactor / propertyFactor;
		}

	}

}