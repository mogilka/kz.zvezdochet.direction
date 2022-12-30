package kz.zvezdochet.direction.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kz.zvezdochet.analytics.service.PlanetHouseService;
import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.service.AspectTypeService;

/**
 * Сервис дирекций планет по астрологическим домам
 * @author Natalie Didenko
 */
public class DirectionService extends PlanetHouseService {

	public DirectionService() {
		String lang = Locale.getDefault().getLanguage();
		tableName = lang.equals("ru") ? "directionhouses" : "us_directionhouses";
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
	 * @param aspect аспект
	 * @return описание позиции планеты в доме
	 * @throws DataAccessException
	 */
	public List<Model> finds(Planet planet, House house, AspectType aspectType) throws DataAccessException {
        List<Model> list = new ArrayList<Model>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		String sql;

		AspectTypeService service = new AspectTypeService();
		if (null == aspectType)
			aspectType = (AspectType)service.find("NEUTRAL");

		try {
			sql = "select * from " + tableName + 
				" where typeid = " + aspectType.getId() +
				" and planetid = " + planet.getId() +
				" and houseid = " + house.getId() +
				" order by aspectid";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
//			System.out.println(planet + " " + house);
			rs = ps.executeQuery();
			while (rs.next())
				list.add(init(rs, create()));
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

	@Override
	public DirectionText init(ResultSet rs, Model model) throws DataAccessException, SQLException {
		DirectionText dict = (model != null) ? (DirectionText)model : (DirectionText)create();
		dict = (DirectionText)super.init(rs, model);
		dict.setDescription(rs.getString("description"));
		dict.setCode(rs.getString("code"));
		dict.setPositive(rs.getBoolean("positive"));
		dict.setRetro(rs.getString("retro"));
		long val = rs.getLong("aspectid");
		if (val > 0)
			dict.setAspect((Aspect)new AspectService().find(val));
		return dict;
	}
}
