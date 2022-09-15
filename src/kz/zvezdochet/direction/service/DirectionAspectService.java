package kz.zvezdochet.direction.service;

import java.sql.ResultSet;
import java.sql.SQLException;

import kz.zvezdochet.analytics.service.PlanetAspectService;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.direction.bean.DirectionAspectText;

/**
 * Сервис дирекций планет
 * @author Natalie Didenko
 */
public class DirectionAspectService extends PlanetAspectService {

	public DirectionAspectService() {
		tableName = "directionaspects";
	}

	@Override
	public Model create() {
		return new DirectionAspectText();
	}

	@Override
	public DirectionAspectText init(ResultSet rs, Model model) throws DataAccessException, SQLException {
		DirectionAspectText dict = (model != null) ? (DirectionAspectText)model : (DirectionAspectText)create();
		dict = (DirectionAspectText)super.init(rs, model);
		dict.setDescription(rs.getString("description"));
		dict.setCode(rs.getString("code"));
		dict.setRetro(rs.getString("retro"));
		return dict;
	}
}
