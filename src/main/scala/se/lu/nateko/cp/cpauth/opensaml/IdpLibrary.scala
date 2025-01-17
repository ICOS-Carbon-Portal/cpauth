// Idp = Identity Provider

package se.lu.nateko.cp.cpauth.opensaml

import java.net.URI
import java.security.PublicKey
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.opensaml.saml.common.xml.SAMLConstants
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor
import org.opensaml.saml.saml2.metadata.EntityDescriptor
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor
import se.lu.nateko.cp.cpauth.utils.Utils.SafeJavaCollectionWrapper
import se.lu.nateko.cp.cpauth.SamlConfig
import se.lu.nateko.cp.cpauth.core.Crypto
import se.lu.nateko.cp.cpauth.utils.Utils
import se.lu.nateko.cp.cpauth.core.CoreUtils
import org.opensaml.security.credential.UsageType
import org.opensaml.saml.ext.saml2mdui.UIInfo
import org.opensaml.saml.saml2.metadata.LocalizedName
import se.lu.nateko.cp.cpauth.core.CpauthException

case class IdpInfo(name: String, id: String)

class IdpProps(val name: String, val keys: Seq[PublicKey], val ssoRedirect: URI)

trait IdpLibrary{

	protected val map: Map[URI, IdpProps]

	def getIdpProps(idpId: URI): Try[IdpProps] = map.get(idpId) match {
		case None => Failure(new CpauthException("Unknown Identity Provider: " + idpId))
		case Some(key) => Success(key)
	}

	def getInfos: Iterable[IdpInfo] = map.map{
		case (id, props) => IdpInfo(props.name, id.toString)
	}

}

object IdpLibrary {

	OpenSamlUtils.bootstrapOpenSaml()
	//Utils.setRootLoggingLevelToInfo()


	def fromConfig(config: SamlConfig): Try[IdpLibrary] = {
		val idpMetaStream = getClass.getResourceAsStream(config.idpMetadataFilePath)

		if(idpMetaStream == null)
			Failure(new Exception(config.idpMetadataFilePath + " file is missing!"))
		else
			Success(fromMetaStream(idpMetaStream))
	}

	def fromMetaStream(metadata: java.io.InputStream): IdpLibrary = {

		val entsDescr = Parser.fromStream[EntitiesDescriptor](metadata)
		val entDescrs = entsDescr.getEntityDescriptors.toSafeIterable

		val urisToProps: Iterable[(URI, IdpProps)] = entDescrs.map(ed => {

			val idp = ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)

			for{
				id <- getId(ed);
				keys <- idpToPublicKeys(idp);
				redirect <- idpToSsoRedirect(idp);
				name <- entityDescrToName(ed).orElse(idpToName(idp, id.toString))
			} yield (id, new IdpProps(name, keys, redirect))

		}).collect{
			case Success(uriToProps) => uriToProps
		}

		new IdpLibrary{
			override val map = Map(urisToProps.toSeq: _*)
		}
	}

	private def idpToPublicKeys(idp: IDPSSODescriptor): Try[Seq[PublicKey]] = for(
		certs <- idpToCertsInBase64(idp);
		keys <- CoreUtils.tryseq(certs.map(Crypto.publicKeyFromX509Cert))
	) yield keys
	
	private def idpToCertsInBase64(idp: IDPSSODescriptor): Try[Seq[String]] = Try{
		import scala.jdk.CollectionConverters._
		idp.getKeyDescriptors.asScala
			.filter(_.getUse != UsageType.ENCRYPTION)
			.map(
				_.getKeyInfo.getX509Datas.get(0).getX509Certificates.get(0).getValue
			)
			.toVector
	}
	
	private def idpToSsoRedirect(idp: IDPSSODescriptor): Try[URI] = Try{
		val redirectSss = idp.getSingleSignOnServices.toSafeIterable.find{
			_.getBinding == SAMLConstants.SAML2_REDIRECT_BINDING_URI
		}
		redirectSss match {
			case None => throw new CpauthException("HTTP-Redirect binding is not supported")
			case Some(sss) => new URI(sss.getLocation)
		}
	}

	private def idpToName(idp: IDPSSODescriptor, fallback: String): Try[String] = Try{
		idp.getExtensions.getOrderedChildren.toSafeIterable.collect{
			case ui: UIInfo => getDisplayName(ui)
		}.flatten.headOption.getOrElse(fallback)
	}

	private def getDisplayName(ui: UIInfo): Option[String] = {
		val candidates = ui.getDisplayNames.toSafeIterable
		getBestName(candidates)
	}

	private def entityDescrToName(ed: EntityDescriptor): Try[String] = Try{
		val candidates = ed.getOrganization.getDisplayNames.toSafeIterable
		getBestName(candidates).get
	}
	
	private def getBestName(names: Iterable[LocalizedName]): Option[String] = {
		val english = names.find(_.getXMLLang == "en").map(_.getValue)
		english.orElse(names.map(_.getValue).headOption)
	}

	private def getId(ed: EntityDescriptor): Try[URI] = Try{
		new URI(ed.getEntityID)
	}

}
