package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.Sign;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.AgePart;
import kz.zvezdochet.service.AspectService;

/**
 * Обработчик расчёта дирекций на указанный возраст
 * @author Natalie Didenko
 */
public class AgeCalcHandler extends Handler {
	private boolean agedp[][][] = null;
	private boolean agedh[][][] = null;
	private TreeMap<Integer, List<SkyPointAspect>> aged = null;
	private String aspectype;
	private boolean retro = false;
	List<Model> aspects = null;
	Event event;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			aged = new TreeMap<>();
			AgePart agePart = (AgePart)activePart.getObject();
			if (!agePart.check(0)) return;
			event = agePart.getEvent();

			Collection<Planet> planets = event.getPlanets().values();
			List<Model> houses = event.getHouses();
			
			updateStatus("Расчёт дирекций на возраст", false);
			List<Model> selplanets = new ArrayList<Model>();
			Planet selplanet = agePart.getPlanet();
			for (Model model : planets) {
				if (selplanet != null) {
					if (selplanet.getId().equals(model.getId())) {
						selplanets.add(model);
						break;
					}
				} else
					selplanets.add(model);
			}

			List<Model> selhouses = new ArrayList<Model>();
			House selhouse = agePart.getHouse();
			if (event.isHousable()) {
				if (selhouse != null)
					for (Model model : houses) {
						if (selhouse.getId().equals(model.getId()))
							selhouses.add(model);
					}
				else
					selhouses.addAll(houses);
			}
			AspectType selaspect = agePart.getAspect();
			if (null == selaspect)
				aspectype = null;
			else
				aspectype = selaspect.getCode();
			
			retro = agePart.getRetro();

			int initage = agePart.getInitialAge();
			int finage = agePart.getFinalAge();
			agedp = new boolean[finage + 2][16][16];
			agedh = new boolean[finage + 2][16][36];
			//инициализируем аспекты
			try {
				aspects = new AspectService().getList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}

			for (int age = initage; age <= finage + 1; age++) {
				//дирекции планеты к другим планетам
				if (null == selhouse) {
					for (Model model : selplanets) {
						Planet selp = (Planet)model;
						for (Model model2 : planets) {
							Planet selp2 = (Planet)model2;
							manageCalc(selp, selp2, age);
						}
					}
				}
				//дирекции планеты к куспидам домов
				if (event.isHousable()) {
					for (Model model2 : selhouses) {
						House selp2 = (House)model2;
						for (Model model : selplanets) {
							Planet selp = (Planet)model;
							manageCalc(selp, selp2, age);
						}
					}
				}
			}
			updateStatus("Расчёт дирекций завершён", false);
			List<SkyPointAspect> list = new ArrayList<SkyPointAspect>();
			for (Entry<Integer, List<SkyPointAspect>> entry : aged.entrySet()) {
				if (entry.getKey() >= initage && entry.getKey() <= finage)
				list.addAll(entry.getValue());
			}
		    agePart.setData(list);
		    agePart.onCalc(false);
			updateStatus("Таблица дирекций сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	/**
	 * Инициализация расчёта дирекций
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 * @param age возраст
	 */
	private void manageCalc(SkyPoint point1, SkyPoint point2, int age) {
		if (point1.getCode().equals(point2.getCode())) return;
		calc(new Planet((Planet)point1), point2, age, false);
		if (retro) {
			if (point2 instanceof Planet && point1 instanceof Planet) {
				//если неретроградная итерация выявила дирекцию по аспекту,
				//пропускаем расчёт ретроградного транзита
				if (!agedp[age][point1.getNumber() - 1][point2.getNumber() - 1])
					calc(new Planet((Planet)point1), point2, age, true);
			} else {
				if (!agedh[age][point1.getNumber() - 1][point2.getNumber() - 1])
					calc(new Planet((Planet)point1), point2, age, true);
			}
		}
	}

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 * @param age возраст
	 * @param retro признак попятного движения
	 */
	private void calc(SkyPoint point1, SkyPoint point2, int age, boolean retro) {
		try {
			//если дирекция между планетами уже рассчитана в первом (неретроградном) заходе,
			//игнорируем (ретроградную) итерацию
			if (retro && point2 instanceof Planet && point1 instanceof Planet
					&& point1.getNumber() > point2.getNumber())
				return;

			//находим угол между точками космограммы с учетом возраста
			double one = CalcUtil.incrementCoord(point1.getLongitude(), age, !retro);
			double two = point2.getLongitude();
			double res = CalcUtil.getDifference(one, two);
			if (point2 instanceof House)
				if (res >= 179 && res < 180)
					++res;

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (aspectype != null && !aspectype.equals(a.getType().getCode()))
					continue;

				if (a.getPlanetid() > 0 && a.getPlanetid() != point1.getId())
					continue;

				if (a.isExact(res)) {
					SkyPointAspect aspect = new SkyPointAspect();
					point1.setLongitude(one);
					initPlanetHouse(point1);
					initPlanetSign(point1);
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					if (point2 instanceof House && CalcUtil.compareAngles(one, two, res)) {
						++res;
						--age;
					}
					aspect.setScore(res);
					aspect.setAge(age);
					aspect.setAspect(a);
					aspect.setRetro(retro);
					aspect.setExact(true);

					List<SkyPointAspect> list = aged.get(age);
					if (null == list)
						list = new ArrayList<SkyPointAspect>();
					list.add(aspect);
					aged.put(age, list);

					if (point2 instanceof Planet && point1 instanceof Planet)
						agedp[age][point1.getNumber() - 1][point2.getNumber() - 1] = true;
					else
						agedh[age][point1.getNumber() - 1][point2.getNumber() - 1] = true;
				}
			}
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber() + ", " + age);
			e.printStackTrace();
		}
	}

	/**
	 * Определяем дом дирекционной планеты
	 * @param skyPoint планета
	 */
	private void initPlanetHouse(SkyPoint skyPoint) {
		List<Model> houseList = event.getHouses();
		Planet planet = (Planet)skyPoint;
		for (int j = 0; j < houseList.size(); j++) { 
			House house = ((House)houseList.get(j));
			int h = (j == houseList.size() - 1) ? 0 : j + 1;
			House house2 = (House)houseList.get(h);
			if (SkyPoint.getHouse(house.getLongitude(), house2.getLongitude(), planet.getLongitude()))
				planet.setHouse(house);
		}
	}

	/**
	 * Определяем знак дирекционной планеты
	 * @param skyPoint планета
	 * @throws DataAccessException 
	 */
	private void initPlanetSign(SkyPoint skyPoint) throws DataAccessException {
		Planet planet = (Planet)skyPoint;
		Sign sign = SkyPoint.getSign(planet.getLongitude(), event.getBirthYear());
		planet.setSign(sign);
	}
}
