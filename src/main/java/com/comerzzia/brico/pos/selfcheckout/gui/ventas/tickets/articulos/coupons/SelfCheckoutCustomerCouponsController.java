package com.comerzzia.brico.pos.selfcheckout.gui.ventas.tickets.articulos.coupons;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.comerzzia.pos.core.gui.InitializeGuiException;
import com.comerzzia.pos.core.gui.componentes.botonaccion.BotonBotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonaccion.imagen.BotonBotoneraImagenCompletaComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.BotoneraComponent;
import com.comerzzia.pos.core.gui.componentes.botonera.ConfiguracionBotonBean;
import com.comerzzia.pos.core.gui.componentes.dialogos.VentanaDialogoComponent;
import com.comerzzia.pos.core.gui.exception.CargarPantallaException;
import com.comerzzia.pos.gui.ventas.tickets.articulos.coupons.CustomerCouponsController;
import com.comerzzia.pos.persistence.fidelizacion.CustomerCouponDTO;
import com.comerzzia.pos.util.i18n.I18N;

import javafx.application.Platform;
import javafx.scene.control.Button;


@SuppressWarnings("unchecked")
@Primary
@Component
public class SelfCheckoutCustomerCouponsController extends CustomerCouponsController {
	
	private Logger log = Logger.getLogger(SelfCheckoutCustomerCouponsController.class);
	
	@Override
	public void initializeForm() throws InitializeGuiException {
		try {
			List<CustomerCouponDTO> availableCoupons = (List<CustomerCouponDTO>) getDatos().get(PARAM_CUSTOMER_COUPONS);
			List<CustomerCouponDTO> activeCoupons = (List<CustomerCouponDTO>) getDatos().get(PARAM_ACTIVE_COUPONS);

			buttonPane.getChildren().clear();

			customerCoupons = new HashMap<String, CustomerCouponDTO>();
			for (CustomerCouponDTO coupon : availableCoupons) {
				customerCoupons.put(coupon.getCouponCode(), coupon);
			}

			this.activeCoupons = new HashMap<String, CustomerCouponDTO>();
			for (CustomerCouponDTO coupon : activeCoupons) {
				this.activeCoupons.put(coupon.getCouponCode(), coupon);
			}

			loadButtons(availableCoupons);
		}
		catch (Exception e) {
			log.error("initializeForm() - Error: " + e.getMessage(), e);
			
			throw new InitializeGuiException(I18N.getTexto("Ha habido un error al mostrar la pantalla de cupones del fidelizado. Por favor, contacte con el administrador."), e);
		}
	}
	
	private void loadButtons(List<CustomerCouponDTO> availableCoupons) throws CargarPantallaException {
		if (availableCoupons != null && !availableCoupons.isEmpty()) {					
			final List<ConfiguracionBotonBean> buttons = new ArrayList<ConfiguracionBotonBean>();

			numRows = new BigDecimal(availableCoupons.size()).divide(new BigDecimal(numColumns), 0, RoundingMode.UP).intValue();	
			
			for(CustomerCouponDTO item : availableCoupons) {
				ConfiguracionBotonBean configuracionBotonBean = new ConfiguracionBotonBean(item.getImageUrl(), item.getCouponName().toUpperCase(), "", item.getCouponCode(), "SET_COUPON_STATE");
				buttons.add(configuracionBotonBean);
			}
			
			if(numRows == 1) {
				for(int i = 0 ; i < numColumns ; i++) {
					buttons.add(new ConfiguracionBotonBean(null, null, null, null, "HUECO"));
				}
				numRows = 2;
			}

			final CustomerCouponsController controller = this;

			Platform.runLater(new Runnable(){

				@Override
				public void run() {
					try {
						double heigth = numRows  * 118.0;
						double extraHeigth = 100;
						if(heigth < scrollPane.getHeight()) {
							heigth = scrollPane.getHeight() - 20;
							extraHeigth = 0;
						}
						double anchura = getStage().getWidth() - 80;
						
						log.trace("loadButtons() - Buttons pane. Height: " + heigth + ". Width: " + anchura);

						BotoneraComponent botoneraAccionesTabla = new BotoneraComponent(numRows, numColumns, controller, buttons, anchura, heigth, BotonBotoneraImagenCompletaComponent.class.getName());
						
						for(Button button : (List<Button>) botoneraAccionesTabla.getListaBotones()) {
							ConfiguracionBotonBean config = (ConfiguracionBotonBean) button.getUserData();
							
							if(!activeCoupons.containsKey(config.getClave())) {
								button.getStyleClass().add(CLASS_BUTTON_DISABLE);
							}
						}
						
						buttonPane.getChildren().add(botoneraAccionesTabla);
						buttonPane.setPrefHeight(heigth + extraHeigth);
						buttonPane.setPrefWidth(anchura);
						disableAll();
					}
					catch (Exception e) {
						log.error("loadButtons() - Error: " + e.getMessage(), e);
					}
				}
			});
		}
		else {
			VentanaDialogoComponent.crearVentanaAviso(I18N.getTexto("No hay cupones disponibles.."), getStage());
		}
	}
	public void disableAll() {
		BotoneraComponent botoneraComponent = (BotoneraComponent) buttonPane.getChildren().get(0);
		for (String couponCode : customerCoupons.keySet()) {
			BotonBotoneraComponent botonAccionado = botoneraComponent.getBotonBotonera(couponCode);

			log.debug("selectDeselectAll() - Se están deseleccionando todos los cupones");
			if (!botonAccionado.getBtAccion().getStyleClass().contains(CLASS_BUTTON_DISABLE)) {
				botonAccionado.getBtAccion().getStyleClass().add(CLASS_BUTTON_DISABLE);
				activeCoupons.remove(couponCode);
			}
			
		}
		tgSelectAll.setSelected(true);
		
		refreshToggleButtonText();
	}
	

}
