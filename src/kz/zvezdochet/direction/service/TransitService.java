package kz.zvezdochet.direction.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.direction.bean.Transit;
import kz.zvezdochet.service.EventService;

/**
 * Сервис транзитов
 * @author Nataly Didenko
 */
public class TransitService extends ModelService {

	public TransitService() {
		tableName = "transit";
	}

	@Override
	public Model save(Model model) throws DataAccessException {
		Transit transit = (Transit)model;
		int result = -1;
        PreparedStatement ps = null;
		try {
			String sql;
			if (null == model.getId()) 
				sql = "insert into " + tableName + " values(0,?,?,?)";
			else
				sql = "update " + tableName + " set " +
					"eventid = ?, " +
					"personid = ?, " +
					"description = ? " +
					"where id = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, transit.getEventid());
			ps.setLong(2, transit.getPersonid());
			ps.setString(3, transit.getDescription());
			if (model.getId() != null) 
				ps.setLong(4, model.getId());

			result = ps.executeUpdate();
			if (1 == result) {
				if (null == model.getId()) { 
					Long autoIncKeyFromApi = -1L;
					ResultSet rsid = ps.getGeneratedKeys();
					if (rsid.next()) {
				        autoIncKeyFromApi = rsid.getLong(1);
				        model.setId(autoIncKeyFromApi);
					    System.out.println(autoIncKeyFromApi + "\t" + ps);
					}
					if (rsid != null)
						rsid.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)	ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return model;
	}

	/**
	 * Поиск транзитных событий персоны
	 * @param personid идентификатор персоны
	 * @return список событий
	 */
	public List<Event> findTransits(Long personid) throws DataAccessException {
		if (null == personid) return null;
		List<Event> list = new ArrayList<Event>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + tableName + " where personid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, personid);
			rs = ps.executeQuery();
			while (rs.next())
				list.add((Event)new EventService().find(rs.getLong("eventid")));
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
	public Model create() {
		return new Transit();
	}

	@Override
	public Model init(ResultSet rs, Model base) throws DataAccessException,
			SQLException {
		// TODO Auto-generated method stub
		return null;
	}
}
