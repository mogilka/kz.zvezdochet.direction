package kz.zvezdochet.direction.handler;

import javax.inject.Named;

import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.direction.part.TransitPart;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Обработчик расчёта транзитов события персоны
 * @author Nataly Didenko
 */
public class TransitCalcHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart, @Named("kz.zvezdochet.direction.commandparameter.today") String today) {
		try {
			updateStatus("Расчёт транзитов", false);
			TransitPart transitPart = (TransitPart)activePart.getObject();
			int itoday = Integer.parseInt(today);
			if (1 == itoday)
				transitPart.initDate();
			else if (2 == itoday)
				transitPart.resetEvent();
			updateStatus("Таблица транзитов сформирована", false);
			transitPart.onCalc(2);
			updateStatus("Карта транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
