package kz.zvezdochet.direction.handler;

import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.direction.part.TransitPart;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Обработчик переключения карты транзитов.
 * По умолчанию отображаются планеты персоны в карте события.
 * Можно переключиться в режим планет события в карте персоны
 * @author Nataly Didenko
 */
public class TransitSwitchHandler extends Handler {
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Переключение карты транзитов", false);
			TransitPart transitPart = (TransitPart)activePart.getObject();
			int mode = transitPart.getModeCalc();
			mode = (0 == mode) ? 1 : 0;
			System.out.println("mode" + mode);
			transitPart.onCalc(mode);
			updateStatus("Карта транзитов переключена", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}