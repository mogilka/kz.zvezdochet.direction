package kz.zvezdochet.direction.handler;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.direction.part.EventPart;

/**
 * Обработчик расчёта транзитов события персоны
 * @author Natalie Didenko
 */
public class EventCalcHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart, @Named("kz.zvezdochet.direction.commandparameter.today") String today) {
		try {
			updateStatus("Расчёт транзитов", false);
			EventPart transitPart = (EventPart)activePart.getObject();
			int itoday = Integer.parseInt(today);
			if (1 == itoday)
				transitPart.initDate();
			else if (2 == itoday)
				transitPart.resetEvent();
			updateStatus("Таблица транзитов сформирована", false);
			transitPart.onCalc(2);
			updateStatus("Карта транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
