package kz.zvezdochet.direction.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kz.zvezdochet.analytics.service.PlanetHouseRuleService;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.direction.bean.DirectionRule;

/**
 * Сервис дирекции планеты к астрологическому дому
 * @author Natalie Didenko
 */
public class DirectionRuleService extends PlanetHouseRuleService {

	public DirectionRuleService() {
		tableName = "directionrule";
	}

	/**
	 * Поиск толкования дирекции планеты к дому
	 * @param planet дирекционная планета
	 * @param house дирекционный астрологический дом
	 * @param type дирекционный тип аспекта
	 * @return массив толкований
	 * @throws DataAccessException
	 */
	public List<DirectionRule> findRules(Planet planet, House house, AspectType type) throws DataAccessException {
		List<DirectionRule> list = new ArrayList<DirectionRule>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		String sql;
		try {
			String whereaspect = "";
			if (2 == type.getId())
				whereaspect = " and typeid in (1,2)";
			else if (3 == type.getId())
				whereaspect = " and typeid in (1,3)";

			sql = "select * from " + tableName + 
				" where planetid = " + planet.getId() +
				" and houseid = " + house.getId()
				+ whereaspect;
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
//			System.out.println(planet + " " + house);
			rs = ps.executeQuery();
			while (rs.next())
				list.add(init(rs, null));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { 
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) { 
				e.printStackTrace(); 
			}
		}
		return list;
	}

	public DirectionRule init(ResultSet rs, Model model) throws DataAccessException, SQLException {
		DirectionRule dict = (model != null) ? (DirectionRule)model : (DirectionRule)create();
		dict = (DirectionRule)super.init(rs, model);
		return dict;
	}

	@Override
	public Model create() {
		return new DirectionRule();
	}

	/**
	 * Поиск толкования дирекционной планеты к натальной
	 * @param planet дирекционная планета
	 * @param house дирекционный астрологический дом
	 * @param type дирекционный тип аспекта
	 * @return толкование
	 * @throws DataAccessException
	 */
	public DirectionRule findRule(Planet planet, House house, AspectType type, Planet planet2, House house2) throws DataAccessException {
        PreparedStatement ps = null;
        ResultSet rs = null;
		String sql;
		try {
			sql = "select * from " + tableName + 
				" where planetid = " + planet.getId() +
				" and houseid = " + house.getId() +
				" and planet2id = " + planet2.getId() +
				" and house2id = " + house2.getId() +
				" and typeid = " + type.getId();
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
//			System.out.println(planet + " " + house);
			rs = ps.executeQuery();
			if (rs.next())
				return init(rs, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { 
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) { 
				e.printStackTrace(); 
			}
		}
		return null;
	}
}
