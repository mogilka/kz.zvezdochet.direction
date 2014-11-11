package kz.zvezdochet.direction.handler;

import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.part.ICalculable;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Обработчик расчёта транзитов события персоны
 * @author Nataly Didenko
 */
public class TransitCalcHandler extends Handler {
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Расчёт транзитов", false);
			ICalculable transitPart = (TransitPart)activePart.getObject();
			transitPart.onCalc(0);
			updateStatus("Карта транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
