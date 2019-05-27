package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
 * Обработчик расчёта транзитов на указанный возраст
 * @author Natalie Didenko
 */
public class AgeCalcHandler extends Handler {
	private boolean agedp[][][] = null;
	private boolean agedh[][][] = null;
	private List<SkyPointAspect> aged = null;
	private String aspectype;
	private boolean retro = false;
	List<Model> aspects = null;
	Event event;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			aged = new ArrayList<SkyPointAspect>();
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
			int finage = agePart.getFinalAge() + 1;
			agedp = new boolean[finage][16][16];
			agedh = new boolean[finage][16][36];
			//инициализируем аспекты
			try {
				aspects = new AspectService().getList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}

			for (int age = initage; age < finage; age++) {
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
		    agePart.setData(aged);
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
		if (!retro) return;
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

			if (agedh[age - 1][point1.getNumber() - 1][point2.getNumber() - 1])
				return;

			//находим угол между точками космограммы с учетом возраста
			double one = makeAge(point1.getLongitude(), age, !retro);
			double two = point2.getLongitude();
			double res = CalcUtil.getDifference(one, two);

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;

				//оппозицию игнорируем, т.к. она получается со сдвигом в один год (ниже искусственно её создаём вслед за соединением)
				if (point2 instanceof House && a.getCode().equals("OPPOSITION"))
					continue;

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
					aspect.setScore(res);
					aspect.setAge(age);
					aspect.setAspect(a);
					aspect.setRetro(retro);
					aspect.setExact(true);
					aged.add(aspect);

					//для соединения создаём искусственную оппозицию в этом же возрасте
					if (point2 instanceof House && a.getCode().equals("CONJUNCTION")) {
						aspect = new SkyPointAspect();
						point1.setLongitude(one);
						aspect.setSkyPoint1(point1);
						aspect.setSkyPoint2(((House)point2).getOpposite());
						aspect.setScore(res + 180);
						aspect.setAge(age);
						aspect.setAspect((Aspect)new AspectService().find("OPPOSITION"));
						aspect.setRetro(retro);
						aspect.setExact(true);
						aged.add(aspect);
					}

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
	 * Преобразуем координату с учётом возраста
	 * @param k координата
	 * @param age возраст
	 * @param increment true|false прибавляем|отнимаем
	 * @return модифицированное значение координаты
	 */
	private double makeAge(double k, int age, boolean increment) {
		double res;
		if (increment) {
			double val = k + age;
			res = (val > 360) ? val - 360 : val;
		} else
			res = (k > age) ? res = k - age : k + 360 - age;
      return res;
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
