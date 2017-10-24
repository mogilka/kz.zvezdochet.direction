package kz.zvezdochet.direction.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kz.zvezdochet.analytics.bean.PlanetHouseText;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.direction.bean.Member;
import kz.zvezdochet.direction.bean.Participant;
import kz.zvezdochet.service.EventService;

/**
 * Сервис прогнозов
 * @author Nataly Didenko
 */
public class CollationService extends ModelService {

	public CollationService() {
		tableName = "collation";
	}

	@Override
	public Model save(Model model) throws DataAccessException {
		Collation collation = (Collation)model;
		int result = -1;
        PreparedStatement ps = null;
		try {
			String sql;
			if (null == model.getId()) 
				sql = "insert into " + tableName + " values(0,?,?,?,?)";
			else
				sql = "update " + tableName + " set " +
					"calculated = ?, " +
					"eventid = ?, " +
					"description = ?, " +
					"created_at = ? " +
					"where id = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setInt(1, collation.isCalculated() ? 1 : 0);
			ps.setLong(2, collation.getEvent().getId());
			ps.setString(3, collation.getDescription());
			ps.setString(4, DateUtil.formatCustomDateTime(new Date(), "yyyy-MM-dd HH:mm:ss"));
			if (model.getId() != null) 
				ps.setLong(5, model.getId());

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
			ps.close();

			if (!collation.isCalculated()) {
				saveParticipants(collation);

				sql = "update " + tableName + " set calculated = 1 where id = ?";
				ps = Connector.getInstance().getConnection().prepareStatement(sql);
				ps.setLong(1, model.getId());
				result = ps.executeUpdate();
				if (1 == result)
					collation.setCalculated(true);
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

	@Override
	public Model create() {
		return new Collation();
	}

	@Override
	public Model init(ResultSet rs, Model base) throws DataAccessException, SQLException {
		Collation model = (base != null) ? (Collation)base : (Collation)create();
		model.setId(Long.parseLong(rs.getString("ID")));
		if (rs.getString("description") != null)
			model.setDescription(rs.getString("description"));
		model.setCreated_at(DateUtil.getDatabaseDateTime(rs.getString("created_at")));
		String s = rs.getString("calculated");
		model.setCalculated(s.equals("1") ? true : false);
		model.setEvent((Event)new EventService().find(rs.getLong("eventid")));
		return model;
	}

	/**
	 * Поиск прогноза по наименованию события
	 * @param text поисковое выражение
	 * @return список прогнозов
	 * @throws DataAccessException
	 */
	public List<Model> findByName(String text) throws DataAccessException {
        List<Model> list = new ArrayList<Model>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select c.* from " + tableName + " c" +
				" inner join " + new EventService().getTableName() + " e on c.eventid = e.id" +
				" where human = 0" +
				" and name like ? " +
				" order by created_at";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setString(1, "%" + text + "%");
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

	/**
	 * Поиск прогнозов событий за период
	 * @param date начальная дата
	 * @param date2 конечная дата
	 * @return список прогнозов
	 * @throws DataAccessException
	 */
	public List<Model> findByDateRange(Date date, Date date2) throws DataAccessException {
        List<Model> list = new ArrayList<Model>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select c.* from " + tableName + " c" +
				" inner join " + new EventService().getTableName() + " e on c.eventid = e.id" +  
				" where human = 0" +
				" and initialdate between ? and ?" +
				" order by created_at";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setString(1, DateUtil.dbdtf.format(date));
			ps.setString(2, DateUtil.dbdtf.format(date2));
			System.out.println(ps);
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

	/**
	 * Добавление участников события
	 * @param model прогноз
	 */
	public void saveParticipants(Collation model) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        int result = -1;
		try {
			String ptable = new ParticipantService().getTableName();
			String mtable = new MemberService().getTableName();
			String patable = getPartAspectTableName();
			String pdtable = getPartDirectionTableName();
			String phtable = getPartHouseTableName();
			String matable = getMemberAspectTableName();
			String mdtable = getMemberDirectionTableName();
			String mhtable = getMemberHouseTableName();
			Connection conn = Connector.getInstance().getConnection();

			//участники
			List<Participant> participants = model.getParticipants();
			if (participants != null && participants.size() > 0) {
				conn.setAutoCommit(false);

				for (Participant part : participants) {
					String sql = "select * from " + ptable + " where id = ?";
					ps = conn.prepareStatement(sql);
					ps.setLong(1, part.getId());
					rs = ps.executeQuery();
					boolean exists = rs.next();
					if (exists)
						sql = "update " + ptable + " set " +
							"collationid = ?, " +
							"eventid = ?, " +
							"win = ? " +
							"where id = ?";
					else
						sql = "insert into " + ptable + " values(0,?,?,?)";
					ps.close();

					ps = conn.prepareStatement(sql);
					ps.setLong(1, model.getId());
					ps.setLong(2, part.getEvent().getId());
					ps.setInt(3, part.isWin() ? 1 : 0);
					if (exists) 
						ps.setLong(4, part.getId());
					result = ps.executeUpdate();
					if (1 == result) {
						if (exists) { 
							Long autoIncKeyFromApi = -1L;
							ResultSet rsid = ps.getGeneratedKeys();
							if (rsid.next()) {
								autoIncKeyFromApi = rsid.getLong(1);
								part.setId(autoIncKeyFromApi);
								System.out.println(autoIncKeyFromApi + "\t" + ps);
							}
							if (rsid != null)
								rsid.close();
						}
					}
					ps.close();

					//аспекты
					sql = "delete from " + patable + " where participantid = ?";
					ps.setLong(1, part.getId());
					ps = conn.prepareStatement(sql);
					ps.execute();
					ps.close();

					List<SkyPointAspect> aspects = part.getAspects();
					if (aspects != null && aspects.size() > 0) {
						sql = "insert into " + patable + "(participantid, planet1id, planet2id, aspectid) values(?,?,?,?)";
						ps = conn.prepareStatement(sql);
						for (SkyPointAspect aspect : aspects) {
							ps.setLong(1, part.getId());
							ps.setLong(2, aspect.getSkyPoint1().getId());
							ps.setLong(3, aspect.getSkyPoint2().getId());
							ps.setLong(4, aspect.getAspect().getId());
							ps.addBatch();
						}
						int[] modified = ps.executeBatch();
						ps.close();
						for (int i = 0; i < modified.length; i++) {
						    if (-2 == modified[i])
						    	System.out.println("Execution " + i + " failed: unknown number of rows modified");
						    else
						    	System.out.println("Execution " + i + " success: " + modified[i] + " rows modified");
						}
					}

					//дирекции
					sql = "delete from " + pdtable + " where participantid = ?";
					ps.setLong(1, part.getId());
					ps = conn.prepareStatement(sql);
					ps.execute();
					ps.close();

					List<SkyPointAspect> dirs = part.getDirections();
					if (dirs != null && dirs.size() > 0) {
						sql = "insert into " + pdtable + "(participantid, planetid, houseid, aspectid) values(?,?,?,?)";
						ps = conn.prepareStatement(sql);
						for (SkyPointAspect aspect : dirs) {
							ps.setLong(1, part.getId());
							ps.setLong(2, aspect.getSkyPoint1().getId());
							ps.setLong(3, aspect.getSkyPoint2().getId());
							ps.setLong(4, aspect.getAspect().getId());
							ps.addBatch();
						}
						int[] modified = ps.executeBatch();
						ps.close();
						for (int i = 0; i < modified.length; i++) {
						    if (-2 == modified[i])
						    	System.out.println("Execution " + i + " failed: unknown number of rows modified");
						    else
						    	System.out.println("Execution " + i + " success: " + modified[i] + " rows modified");
						}
					}

					//дома
					sql = "delete from " + phtable + " where participantid = ?";
					ps.setLong(1, part.getId());
					ps = conn.prepareStatement(sql);
					ps.execute();
					ps.close();

					List<PlanetHouseText> houses = part.getHouses();
					if (houses != null && houses.size() > 0) {
						sql = "insert into " + phtable + "(participantid, planetid, houseid) values(?,?,?)";
						ps = conn.prepareStatement(sql);
						for (PlanetHouseText aspect : houses) {
							ps.setLong(1, part.getId());
							ps.setLong(2, aspect.getPlanet().getId());
							ps.setLong(3, aspect.getHouse().getId());
							ps.addBatch();
						}
						int[] modified = ps.executeBatch();
						ps.close();
						for (int i = 0; i < modified.length; i++) {
						    if (-2 == modified[i])
						    	System.out.println("Execution " + i + " failed: unknown number of rows modified");
						    else
						    	System.out.println("Execution " + i + " success: " + modified[i] + " rows modified");
						}
					}

					//фигуранты
					List<Member> members = part.getMembers();
					if (members != null && members.size() > 0) {
						for (Member member : members) {
							sql = "select * from " + mtable + " where id = ?";
							ps = Connector.getInstance().getConnection().prepareStatement(sql);
							ps.setLong(1, member.getId());
							rs = ps.executeQuery();
							exists = rs.next();
							if (exists)
								sql = "update " + mtable + " set " +
									"eventid = ?, " +
									"participantid = ?, " +
									"hit = ?, " +
									"pass = ?, " +
									"miss = ?, " +
									"save = ?, " +
									"foul = ?, " +
									"substitute = ?, " +
									"injury = ? " +
									"where id = ?";
							else
								sql = "insert into " + mtable + " values(0,?,?,?,?,?,?,?,?,?)";
							ps.close();

							ps = conn.prepareStatement(sql);
							ps.setLong(1, member.getEvent().getId());
							ps.setLong(2, part.getId());
							ps.setInt(3, member.isHit() ? 1 : 0);
							ps.setInt(4, member.isPass() ? 1 : 0);
							ps.setInt(5, member.isMiss() ? 1 : 0);
							ps.setInt(6, member.isSave() ? 1 : 0);
							ps.setInt(7, member.isFoul() ? 1 : 0);
							ps.setInt(8, member.isSubstitute() ? 1 : 0);
							ps.setInt(9, member.isInjury() ? 1 : 0);
							if (exists) 
								ps.setLong(10, member.getId());
							result = ps.executeUpdate();
							if (1 == result) {
								if (exists) { 
									Long autoIncKeyFromApi = -1L;
									ResultSet rsid = ps.getGeneratedKeys();
									if (rsid.next()) {
										autoIncKeyFromApi = rsid.getLong(1);
										member.setId(autoIncKeyFromApi);
										System.out.println(autoIncKeyFromApi + "\t" + ps);
									}
									if (rsid != null)
										rsid.close();
								}
							}
							ps.close();
							
							//аспекты
							sql = "delete from " + matable + " where memberid = ?";
							ps.setLong(1, member.getId());
							ps = conn.prepareStatement(sql);
							ps.execute();
							ps.close();

							aspects = member.getAspects();
							if (aspects != null && aspects.size() > 0) {
								sql = "insert into " + matable + "(memberid, planet1id, planet2id, aspectid) values(?,?,?,?)";
								ps = conn.prepareStatement(sql);
								for (SkyPointAspect aspect : aspects) {
									ps.setLong(1, member.getId());
									ps.setLong(2, aspect.getSkyPoint1().getId());
									ps.setLong(3, aspect.getSkyPoint2().getId());
									ps.setLong(4, aspect.getAspect().getId());
									ps.addBatch();
								}
								int[] modified = ps.executeBatch();
								ps.close();
								for (int i = 0; i < modified.length; i++) {
								    if (-2 == modified[i])
								    	System.out.println("Execution " + i + " failed: unknown number of rows modified");
								    else
								    	System.out.println("Execution " + i + " success: " + modified[i] + " rows modified");
								}
							}

							//дирекции
							sql = "delete from " + mdtable + " where memberid = ?";
							ps.setLong(1, member.getId());
							ps = conn.prepareStatement(sql);
							ps.execute();
							ps.close();

							dirs = member.getDirections();
							if (dirs != null && dirs.size() > 0) {
								sql = "insert into " + mdtable + "(memberid, planetid, houseid, aspectid) values(?,?,?,?)";
								ps = conn.prepareStatement(sql);
								for (SkyPointAspect aspect : dirs) {
									ps.setLong(1, member.getId());
									ps.setLong(2, aspect.getSkyPoint1().getId());
									ps.setLong(3, aspect.getSkyPoint2().getId());
									ps.setLong(4, aspect.getAspect().getId());
									ps.addBatch();
								}
								int[] modified = ps.executeBatch();
								ps.close();
								for (int i = 0; i < modified.length; i++) {
								    if (-2 == modified[i])
								    	System.out.println("Execution " + i + " failed: unknown number of rows modified");
								    else
								    	System.out.println("Execution " + i + " success: " + modified[i] + " rows modified");
								}
							}

							//дома
							sql = "delete from " + mhtable + " where memberid = ?";
							ps.setLong(1, member.getId());
							ps = conn.prepareStatement(sql);
							ps.execute();
							ps.close();

							houses = member.getHouses();
							if (houses != null && houses.size() > 0) {
								sql = "insert into " + mhtable + "(memberid, planetid, houseid) values(?,?,?)";
								ps = conn.prepareStatement(sql);
								for (PlanetHouseText aspect : houses) {
									ps.setLong(1, member.getId());
									ps.setLong(2, aspect.getPlanet().getId());
									ps.setLong(3, aspect.getHouse().getId());
									ps.addBatch();
								}
								int[] modified = ps.executeBatch();
								ps.close();
								for (int i = 0; i < modified.length; i++) {
								    if (-2 == modified[i])
								    	System.out.println("Execution " + i + " failed: unknown number of rows modified");
								    else
								    	System.out.println("Execution " + i + " success: " + modified[i] + " rows modified");
								}
							}
						}
					}
				}
				conn.commit();
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
	}

	private String getPartAspectTableName() {
		return "participant_aspects";
	}
	private String getPartDirectionTableName() {
		return "participant_directions";
	}
	private String getPartHouseTableName() {
		return "participant_houses";
	}
	private String getMemberAspectTableName() {
		return "member_aspects";
	}
	private String getMemberDirectionTableName() {
		return "member_directions";
	}
	private String getMemberHouseTableName() {
		return "member_houses";
	}
}
