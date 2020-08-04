package kz.zvezdochet.direction.handler;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.DirectionsPart;
import kz.zvezdochet.part.EventPart;

/**
 * Отображение сводной таблицы дирекций планет по домам
 * @author Natalie Didenko
 */
public class DirectionsHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart eventPart = (EventPart)activePart.getObject();
			Event event = (Event)eventPart.getModel(EventPart.MODE_ASPECT_PLANET_PLANET, true);
			if (null == event) return;
			if (null == event.getHouses()) return; //TODO выдавать сообщение
			updateStatus("Расчёт дирекций планет по домам", false);

			Collection<Planet> planets = event.getPlanets().values();
			Map<Long, House> houses = event.getHouses();
			int hcount = houses.size();
			int pcount = planets.size();
			String[][] data = new String[hcount][pcount + 1];
			//заполняем заголовки строк названиями куспидов и третей домов и их координатами
			for (House house : houses.values())
				data[house.getNumber() - 1][0] = house.getName() + " (" + CalcUtil.roundTo(house.getLongitude(), 2) + ")";

			//формируем массив дирекций планет по домам
			for (Planet planet : planets) {
				double one = planet.getLongitude();
				for (House house : houses.values()) {
					double two = house.getLongitude();
					double res;
					boolean retro = false;
					double diff = one - two;
					if (diff > 0) {
						if (diff < 189) {
							res = diff;
							retro = true;
						} else
							res = 360 - one + two;
					} else {
						diff = two - one;
						if (diff < 189)
							res = diff;
						else {
							res = 360 - two + one;
							retro = true;
						}
					}
					if (retro)
						res *= -1;
					if (Math.abs(res) < 100) //TODO корректировать лимит возраста по дате смерти? =)
						data[house.getNumber() - 1][planet.getId().intValue() - 18] = String.valueOf(CalcUtil.roundTo(res, 2));
				}
			}
			updateStatus("Расчёт дирекций завершён", false);
			MPart part = partService.findPart("kz.zvezdochet.direction.part.directions");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    DirectionsPart housePart = (DirectionsPart)part.getObject();
		    housePart.setEvent(event);
		    housePart.setData(data);
			updateStatus("Таблица дирекций сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
