package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.direction.part.CollationPart;
import kz.zvezdochet.direction.service.CollationService;
import kz.zvezdochet.service.AspectService;

/**
 * Расчёт группового прогноза
 * @author Nataly Didenko
 *
 */
public class CalcCollationHandler extends Handler {
	private StringBuffer atext;
	private StringBuffer ptext;
	private StringBuffer dtext;
	private StringBuffer htext;
	private StringBuffer mtext;

	@SuppressWarnings("unchecked")
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Групповой расчёт", false);
			atext = new StringBuffer();
			ptext = new StringBuffer();
			dtext = new StringBuffer();
			htext = new StringBuffer();
			mtext = new StringBuffer();

			CollationPart collationPart = (CollationPart)activePart.getObject();
			Collation collation = (Collation)collationPart.getModel(0, false);
			if (null == collation) {
				DialogUtil.alertWarning("Выберите или создайте групповой прогноз для расчёта");
				return;
			}

			Event event = collation.getEvent();
			if (null == collation.getEvent()) {
				DialogUtil.alertWarning("Выберите событие для расчёта");
				return;
			}
			if (!event.isCalculated()) {
				event.calc(true);
				updateStatus("Расчётная конфигурация события создана", false);
			}

			List<Event> participants = collation.getParticipants();
			if (null == participants || 0 == participants.size()) {
				DialogUtil.alertInfo("Добавьте участников события");
				return;
			}

			aspects = new AspectService().getMajorList();
			for (Event participant : participants) {
				mtext.append(participant.getName() + "\n");
				Map<String, Integer> aspectMap = new HashMap<String, Integer>();
				Map<String, Object> planetMap = new HashMap<String, Object>();
				Map<String, Object> dirMap = new HashMap<String, Object>();
				Map<String, Object> houseMap = new HashMap<String, Object>();
				Map<String, Object> map = new HashMap<String, Object>();

				if (participant.getRectification() != 3) {
					if (!participant.isCalculated()) {
						participant.calc(false);
						updateStatus("Расчётная конфигурация " + participant.getName() + " создана", false);
					}
					map = makeTransits(event, participant);
				}
				List<Event> members = participant.getMembers();
				if (members != null && members.size() > 0) {
					Map<String, Object> map2 = new HashMap<String, Object>();
					Map<String, Integer> amap = new HashMap<String, Integer>();
					Map<String, Object> pmap = new HashMap<String, Object>();
					Map<String, Object> dmap = new HashMap<String, Object>();

					for (Event member : members) {
						if (!member.isCalculated()) {
							member.calc(false);
							updateStatus("Расчётная конфигурация " + member.getName() + " создана", false);
						}
						mtext.append("\t" + member.getName() + "\n");

						//суммируем данные всех фигурантов участника
						map2 = makeTransits(event, member);

						//аспекты
						amap = (Map<String, Integer>)map2.get("Аспекты");
						for (Map.Entry<String, Integer> entry : amap.entrySet()) {
						    String key = entry.getKey();
						    int val = entry.getValue();
							int total = aspectMap.containsKey(key) ? aspectMap.get(key) : 0;
							aspectMap.put(key, total + val);
						}

						//планеты
						pmap = (Map<String, Object>)map2.get("Планеты");
						for (Map.Entry<String, Object> entry : pmap.entrySet()) {
						    String key = entry.getKey();
						    int[] mvals = (int[])pmap.get(key);
						    int[] vals = planetMap.containsKey(key) ? (int[])planetMap.get(key) : new int[3];
						    for (int i = 0; i < 3; i++)
						    	vals[i] += mvals[i];
							planetMap.put(key, vals);
						}

						//дирекции
						dmap = (Map<String, Object>)map2.get("Дирекции");
						for (Map.Entry<String, Object> entry : dmap.entrySet()) {
						    String key = entry.getKey();
						    int[] mvals = (int[])dmap.get(key);
						    int[] vals = dirMap.containsKey(key) ? (int[])dirMap.get(key) : new int[3];
						    for (int i = 0; i < 3; i++)
						    	vals[i] += mvals[i];
						    dirMap.put(key, vals);
						}

						//дома
						houseMap = (Map<String, Object>)map2.get("Дома");
					}
				} else {
					aspectMap = (Map<String, Integer>)map.get("Аспекты");
					planetMap = (Map<String, Object>)map.get("Планеты");
					dirMap = (Map<String, Object>)map.get("Дирекции");
					houseMap = (Map<String, Object>)map.get("Дома");
				}
				//аспекты
				atext.append("Аспекты " + participant.getName() + "\n");
				for (String key : aspectMap.keySet()) {
					String t = key.length() < 5 ? "\t" : "";
					atext.append("\t" + key + "\t" + t + aspectMap.get(key) + "\n");
				}
				atext.append("\n");

				//планеты
				ptext.append("Планеты " + participant.getName() + "\n");
				String[] labels = {"=", "-", "+"};
				for (String key : planetMap.keySet()) {
					int[] vals = (int[])planetMap.get(key);
					ptext.append("\t" + key);
					int total = 0;
					for (int i = 0; i < 3; i++) {
						String t = (i < 1 && key.length() < 7) ? "\t" : "";
						ptext.append("\t" + t + labels[i] + vals[i]);
						int val = vals[i];
						if (1 == i)
							val *= -1;
						total += val;
					}
					ptext.append("\t\t" + total + "\n");
				}
				ptext.append("\n");

				//дирекции
				dtext.append("Дирекции " + participant.getName() + "\n");
				for (String key : dirMap.keySet()) {
					int[] vals = (int[])dirMap.get(key);
					dtext.append("\t" + key);
					int total = 0;
					for (int i = 0; i < 3; i++) {
						String t = "";
						if (i < 1) {
							int strlen = key.length();
							if (strlen < 15)
								t += "\t";
							if (strlen < 10)
								t += "\t";
							if (strlen < 7)
								t += "\t";
						}
						dtext.append("\t" + t + labels[i] + vals[i]);
						int val = vals[i];
						if (1 == i)
							val *= -1;
						total += val;
					}
					dtext.append("\t\t" + total + "\n");
				}
				dtext.append("\n");

				//дома
				htext.append("Дома " + participant.getName() + "\n");
				for (String key : houseMap.keySet())
					htext.append("\t" + key + ": " + houseMap.get(key) + "\n");
				htext.append("\n");
			}
			updateStatus("Групповой прогноз сформирован", false);

			String text = atext + "\n" + ptext + "\n" + dtext + "\n" + mtext + "\n" + htext;
			collationPart.onCalc(text);
			List<Object> params = new ArrayList<Object>();
			params.add(new Object[] {"text", "String", text});
			new CollationService().update(collation.getId(), params);
			updateStatus("Групповой прогноз сохранён", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	private List<Model> aspects;

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private Aspect calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим транзитный угол
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
	
			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExact(res)) {
					mtext.append("\t\t" + point1.getName() + " " + a.getType().getSymbol() + " " + point2.getName() + " > " + res + "\n");
					return a;
				}
			}
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Расчёт транзитов
	 */
	private Map<String, Object> makeTransits(Event event, Event person) {
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Integer> aspectMap = new HashMap<String, Integer>();
		Map<String, Object> planetMap = new HashMap<String, Object>();
		Map<String, Object> dirMap = new HashMap<String, Object>();
		Map<String, Object> houseMap = new HashMap<String, Object>();

		Long[] pfilter = Planet.getSportSet();
		Long[] hfilter = House.getSportSet();

		if (null == person.getConfiguration())
			person.init(false);
		if (null == event.getConfiguration())
			event.init(false);
		List<Model> planets = person.getConfiguration().getPlanets();
		List<Model> planets2 = event.getConfiguration().getPlanets();
		List<Model> houses = event.getConfiguration().getHouses();

		for (Model model : planets) {
			if (!Arrays.asList(pfilter).contains(model.getId()))
				continue;
			Planet planet = (Planet)model;
			//дирекции планеты участника к планетам события
			for (Model model2 : planets2) {
				if (!Arrays.asList(pfilter).contains(model2.getId()))
					continue;
				Planet eplanet = (Planet)model2;
				Aspect aspect = calc(planet, eplanet);
				if (aspect != null) {
					//статистика аспектов
					String key = aspect.getName();
					int val = aspectMap.containsKey(key) ? aspectMap.get(key) : 0;
					aspectMap.put(key, ++val);

					//статистика планет
					key = planet.getName();
					int[] vals = planetMap.containsKey(key) ? (int[])planetMap.get(key) : new int[3];
					int typeid = (int)aspect.getTypeid();
					int index = typeid - 1;
					if (1 == typeid) {
						if ("Lilith" == planet.getCode() || "Lilith" == eplanet.getCode()
								|| "Kethu" == planet.getCode() || "Kethu" == eplanet.getCode())
							vals[1] += 1;
						else
							vals[index] += 1;
					} else
						vals[index] += 1;
					planetMap.put(key, vals);
				}
			}
			//дирекции планеты участника к куспидам домов события
			for (Model model2 : houses) {
				if (!Arrays.asList(hfilter).contains(model2.getId()))
					continue;
				House house = (House)model2;
				Aspect aspect = calc(planet, house);
				if (aspect != null) {
					//статистика домов
					String key = house.getName();
					int[] vals = dirMap.containsKey(key) ? (int[])dirMap.get(key) : new int[3];
					int typeid = (int)aspect.getTypeid();
					int index = typeid - 1;
					if (1 == typeid) {
						if ("Lilith" == planet.getCode() || "Kethu" == planet.getCode())
							vals[1] += 1;
						else
							vals[index] += 1;
					} else
						vals[index] += 1;
					dirMap.put(key, vals);
				}
			}
		}
		//планеты участника в домах события
		for (Model model : planets) {
			Planet planet = (Planet)model;
			for (int j = 0; j < houses.size(); j++) {
				House house = (House)houses.get(j);
				if (!Arrays.asList(hfilter).contains(house.getId()))
					continue;
				double pcoord = planet.getCoord();
				Double hmargin = (j == houses.size() - 1) ?
					((House)houses.get(0)).getCoord() : 
					((House)houses.get(j + 1)).getCoord();
				double[] res = CalcUtil.checkMarginalValues(house.getCoord(), hmargin, pcoord);
				hmargin = res[0];
				pcoord = res[1];
				//если градус планеты находится в пределах куспидов
				//текущей и предыдущей трети домов,
				//запоминаем, в каком доме находится планета
				if (Math.abs(pcoord) < hmargin & 
						Math.abs(pcoord) >= house.getCoord()) {
					String key = house.getName();
					String vals = houseMap.containsKey(key) ? (String)houseMap.get(key) : "";
					vals += planet.getName() + " ";
					houseMap.put(key, vals);
					break;
				}
			}
		}
		map.put("Аспекты", aspectMap);
		map.put("Планеты", planetMap);
		map.put("Дирекции", dirMap);
		map.put("Дома", houseMap);
		return map;
	}
}
