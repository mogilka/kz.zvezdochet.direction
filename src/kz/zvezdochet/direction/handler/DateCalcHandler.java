package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import kz.zvezdochet.direction.part.DatePart;
import kz.zvezdochet.service.AspectService;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Обработчик расчёта транзитов на указанную дату.
 * Двигаем натальные планеты от первоначального положения
 * на количество градусов, соответствующих текущему возрасту персоны,
 * и анализируем данные как транзиты
 * @author Nataly Didenko
 */
public class DateCalcHandler extends Handler {
	protected List<SkyPointAspect> aged = new ArrayList<SkyPointAspect>();

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			DatePart datePart = (DatePart)activePart.getObject();
			if (!datePart.check(0)) return;
			Event person = datePart.getPerson();

			updateStatus("Расчёт транзитов на указанную дату", false);
			Date seldate = datePart.getDate();
			Event event = new Event();
			event.setBirth(seldate);
			event.calc(false);

			makeTransits(person, event);
		    datePart.setData(aged);
			updateStatus("Таблица транзитов сформирована", false);

			datePart.setEvent(event);
			datePart.onCalc(0);
			updateStatus("Космограмма транзитов сформирована", false);
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
