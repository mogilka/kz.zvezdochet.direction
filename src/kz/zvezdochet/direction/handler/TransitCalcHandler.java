package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.service.AspectService;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Обработчик расчёта транзитов события персоны
 * @author Nataly Didenko
 */
public class TransitCalcHandler extends Handler {
	private List<SkyPointAspect> aged;

	@Execute
	public void execute(@Active MPart activePart, @Named("kz.zvezdochet.direction.commandparameter.today") String today) {
		try {
			aged = new ArrayList<SkyPointAspect>();
			updateStatus("Расчёт транзитов", false);
			TransitPart transitPart = (TransitPart)activePart.getObject();
			if (1 == Integer.parseInt(today))
				transitPart.initDate();
			makeTransits(transitPart.getPerson(), transitPart.getModel());
			transitPart.setTransitData(aged);
			System.out.println("aged\t" + aged.size());
			updateStatus("Таблица транзитов сформирована", false);
			transitPart.onCalc(2);
			updateStatus("Карта транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private void calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим угол между точками космограммы с учетом возраста
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
	
			//определяем, является ли аспект стандартным
			List<Model> aspects = null;
			try {
				aspects = new AspectService().getList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExactTruncAspect(res)) {
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAspect(a);
					aged.add(aspect);
				}
			}
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
	}

	/**
	 * Расчёт транзитов
	 * @param person персона
	 * @param event событие
	 */
	protected void makeTransits(Event person, Event event) {
		//дирекции планеты к другим планетам
		List<Model> trplanets = event.getConfiguration().getPlanets();
		for (Model model : trplanets) {
			Planet trplanet = (Planet)model;
			for (Model model2 : person.getConfiguration().getPlanets()) {
				Planet planet = (Planet)model2;
				calc(trplanet, planet);
			}
		}

		//дирекции планеты к куспидам домов
		for (Model model : trplanets) {
			Planet trplanet = (Planet)model;
			for (Model model2 : person.getConfiguration().getHouses()) {
				House house = (House)model2;
				calc(trplanet, house);
			}
		}
		updateStatus("Расчёт транзитов завершён", false);
	}
}
