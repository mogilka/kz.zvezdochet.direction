package kz.zvezdochet.direction.handler;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.direction.part.MonthPart;

/**
 * Обработчик расчёта транзитов месяца
 * @author Natalie Didenko
 */
public class MonthCalcHandler extends Handler {
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Расчёт транзитов", false);
			MonthPart transitPart = (MonthPart)activePart.getObject();
			updateStatus("Таблица транзитов сформирована", false);
			transitPart.onCalc(0);
			updateStatus("Карта транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
