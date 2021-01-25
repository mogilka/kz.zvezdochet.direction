package kz.zvezdochet.direction.handler;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.Sign;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.SignsPart;
import kz.zvezdochet.part.EventPart;
import kz.zvezdochet.service.SignService;

/**
 * Отображение сводной таблицы дирекций планет по знакам
 * @author Natalie Didenko
 */
public class SignsHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart eventPart = (EventPart)activePart.getObject();
			Event event = (Event)eventPart.getModel(EventPart.MODE_ASPECT_PLANET_PLANET, true);
			if (null == event) return;
			updateStatus("Расчёт дирекций планет по знакам", false);

			Collection<Planet> planets = event.getPlanets().values();
			List<Sign> sings = new SignService().getZodiac();
			int scount = sings.size();
			int pcount = planets.size();
			String[][] data = new String[scount][pcount + 1];
			//заполняем заголовки строк названиями знаков
			for (Sign sign : sings)
				data[sign.getNumber() - 1][0] = sign.getName() + " (" + CalcUtil.roundTo(sign.getStart(event.getBirthYear()), 2) + ")";

			int MAX_AGE = event.isHuman() ? 100 : 500; //TODO корректировать лимит возраста по дате смерти? =)
			//формируем массив дирекций планет по домам
			for (Planet planet : planets) {
				double one = planet.getLongitude();
				for (Sign sign : sings) {
					double two = sign.getStart(event.getBirthYear());
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
					if (Math.abs(res) < MAX_AGE)
						data[sign.getNumber() - 1][planet.getId().intValue() - 18] = String.valueOf(CalcUtil.roundTo(res, 2));
				}
			}
			updateStatus("Расчёт дирекций завершён", false);
			MPart part = partService.findPart("kz.zvezdochet.direction.part.signs");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    SignsPart signPart = (SignsPart)part.getObject();
		    signPart.setEvent(event);
		    signPart.setData(data);
			updateStatus("Таблица дирекций сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
