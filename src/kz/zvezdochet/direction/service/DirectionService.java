package kz.zvezdochet.direction.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import kz.zvezdochet.analytics.service.PlanetHouseService;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.service.AspectTypeService;

/**
 * Сервис дирекций планет по астрологическим домам
 * @author Natalie Didenko
 */
public class DirectionService extends PlanetHouseService {

	public DirectionService() {
		tableName = "directionhouses";
	}

	@Override
	public Model create() {
		return new DirectionText();
	}

	/**
	 * Поиск толкования планеты в доме
	 * @param planet планета
	 * @param house астрологический дом
	 * @param aspectType тип аспекта
	 * @return описание позиции планеты в доме
	 * @throws DataAccessException
	 */
	public Model find(Planet planet, House house, AspectType aspectType) throws DataAccessException {
        PreparedStatement ps = null;
        ResultSet rs = null;
		String sql;

		AspectTypeService service = new AspectTypeService();
		String pcode = planet.getCode();
		if (null == aspectType) {
			if (planet.isDamaged() || planet.isLilithed() || pcode.equals("Lilith"))
				aspectType = (AspectType)service.find("NEGATIVE");
			else
				aspectType = (AspectType)service.find("NEUTRAL");
		}
		if (aspectType.getCode().equals("NEUTRAL")) {
			if (pcode.equals("Kethu") || pcode.equals("Lilith"))
				aspectType = (AspectType)service.find("NEGATIVE");
			else if (pcode.equals("Selena"))
				aspectType = (AspectType)service.find("POSITIVE");
		}		
		try {
			sql = "select * from " + tableName + 
				" where typeid = " + aspectType.getId() +
				" and planetid = " + planet.getId() +
				" and houseid = " + house.getId();
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
//			System.out.println(planet + " " + house);
			rs = ps.executeQuery();
			if (rs.next())
				return init(rs, create());
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

	@Override
	public DirectionText init(ResultSet rs, Model model) throws DataAccessException, SQLException {
		DirectionText dict = (model != null) ? (DirectionText)model : (DirectionText)create();
		dict = (DirectionText)super.init(rs, model);
		dict.setDescription(rs.getString("description"));
		dict.setCode(rs.getString("code"));
		return dict;
	}
}
