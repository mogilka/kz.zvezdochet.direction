package kz.zvezdochet.direction.provider;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.ui.ArrayLabelProvider;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Формат таблицы аспектов
 * @author Nataly Didenko
 */
public class TransitLabelProvider extends ArrayLabelProvider implements ITableLabelProvider {

	@Override
	public String getColumnText(Object element, int columnIndex) {
		SkyPointAspect aspect = (SkyPointAspect)element;
		Aspect realasp = aspect.getAspect();
		switch (columnIndex) {
			case 0: return String.valueOf(aspect.getAge());
			case 1: return aspect.getSkyPoint1().getName();
			case 2: return (null == realasp) ? "" : realasp.getName();
			case 3: return aspect.getSkyPoint2().getName();
			case 4: return aspect.isRetro() ? "R" : "";
			case 5: return String.valueOf(aspect.getScore());
		}
		return null;
	}
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		SkyPointAspect aspect = (SkyPointAspect)element;
		switch (columnIndex) {
			case 1: return aspect.getSkyPoint1() instanceof Planet
					? ((Planet)aspect.getSkyPoint1()).getImage() : null;
			case 3: return aspect.getSkyPoint2() instanceof Planet
					? ((Planet)aspect.getSkyPoint2()).getImage() : null;
		}
		return null;
	}
	@Override
	public Color getForeground(Object element, int columnIndex) {
		if (2 == columnIndex) {
			SkyPointAspect aspect = (SkyPointAspect)element;
			if (aspect.getAspect() != null)
				return aspect.getAspect().getType().getColor();
		}
		return  Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	}
}
