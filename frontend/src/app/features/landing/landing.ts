import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';

interface Feature {
  icon: string;
  title: string;
  text: string;
}

interface Plan {
  name: string;
  price: string;
  unit: string;
  pitch: string;
  features: string[];
  highlighted: boolean;
  cta: string;
}

interface Question {
  q: string;
  a: string;
}

/**
 * Page d accueil commerciale.
 *
 * C est la vitrine du produit : elle s adresse au dirigeant d une societe
 * de maintenance, pas a l utilisateur quotidien. Elle enonce donc le
 * probleme avant la solution, montre un resultat chiffre, et conduit a un
 * seul geste : demander une demonstration.
 *
 * L appel a l action ouvre un courriel pre-redige plutot qu un formulaire :
 * tant qu aucun service d envoi n est en place, un formulaire donnerait
 * l illusion d un message parti alors que personne ne le recevrait.
 */
@Component({
  selector: 'app-landing',
  imports: [RouterLink, Icon, Logo],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class LandingPage {
  protected readonly year = new Date().getFullYear();

  /**
   * Courriel pre-redige pour demander une demonstration.
   * Le sujet et le corps sont amorces : le prospect n a plus qu a
   * completer et envoyer.
   */
  protected readonly contactLink =
    `mailto:${environment.contactEmail}` +
    '?subject=' +
    encodeURIComponent('Demande de demonstration — Depanio') +
    '&body=' +
    encodeURIComponent(
      'Bonjour,\n\n' +
        'Je souhaite voir Depanio en fonctionnement.\n\n' +
        'Societe :\n' +
        'Nombre d etablissements suivis :\n' +
        'Telephone :\n\n' +
        'Merci.',
    );

  /** Question dont la reponse est depliee, null si toutes sont fermees. */
  protected readonly openQuestion = signal<number | null>(0);

  protected readonly features: Feature[] = [
    {
      icon: 'ticket',
      title: 'Chaque appel devient un ticket',
      text:
        'Un numero, une priorite, un responsable, une echeance. Plus rien ne ' +
        'depend de la memoire de celui qui a decroche le telephone.',
    },
    {
      icon: 'clock',
      title: 'Des delais que vous tenez',
      text:
        'Le delai de resolution est calcule des la declaration. Le technicien ' +
        'est prevenu avant l echeance, et tout depassement est constate ' +
        'automatiquement.',
    },
    {
      icon: 'qr',
      title: 'Une etiquette QR par machine',
      text:
        'Le gerant scanne le code colle sur la caisse, decrit la panne, et ' +
        'c est declare. Pas de compte a chercher, pas de liste a parcourir.',
    },
    {
      icon: 'chart',
      title: 'Des chiffres pour vos contrats',
      text:
        'Delai moyen de resolution, taux de respect des engagements, materiel ' +
        'le plus defaillant. De quoi renegocier en position de force.',
    },
    {
      icon: 'bell',
      title: 'Tout le monde au courant, en direct',
      text:
        'Affectation, changement de statut, nouveau message : chacun recoit ' +
        'ce qui le concerne, immediatement.',
    },
    {
      icon: 'shield',
      title: 'Chaque client chez lui',
      text:
        'Deux etablissements concurrents peuvent utiliser la meme plateforme ' +
        'sans jamais voir les donnees de l autre.',
    },
  ];

  protected readonly steps = [
    {
      number: '01',
      title: 'Le gerant signale',
      text: 'Depuis son telephone, en scannant l etiquette de la machine ou en quelques champs.',
    },
    {
      number: '02',
      title: 'Vous affectez',
      text: 'La file d attente affiche les urgences en tete. Un clic pour confier le ticket au bon technicien.',
    },
    {
      number: '03',
      title: 'Le technicien intervient',
      text: 'Il suit l avancement, echange avec le client, et documente la solution appliquee.',
    },
    {
      number: '04',
      title: 'Le client confirme',
      text: 'Il valide la resolution et note la prestation. Le dossier est clos, et archive.',
    },
  ];

  protected readonly plans: Plan[] = [
    {
      name: 'Essentiel',
      price: '89',
      unit: 'DT / mois',
      pitch: 'Pour une societe qui demarre son activite de maintenance.',
      features: [
        "Jusqu'a 10 etablissements clients",
        'Tickets et echanges illimites',
        'Delais contractuels et alertes',
        'Etiquettes QR illimitees',
        'Tableau de bord',
        'Assistance par courriel',
      ],
      highlighted: false,
      cta: 'Commencer',
    },
    {
      name: 'Professionnel',
      price: '219',
      unit: 'DT / mois',
      pitch: 'Pour une equipe structuree avec plusieurs techniciens.',
      features: [
        'Etablissements clients illimites',
        'Tout le contenu de la formule Essentiel',
        'Charge de travail par technicien',
        'Historique complet et piste d audit',
        'Export des donnees',
        'Assistance prioritaire',
      ],
      highlighted: true,
      cta: 'Demander une demonstration',
    },
    {
      name: 'Sur mesure',
      price: 'Nous consulter',
      unit: '',
      pitch: 'Pour un deploiement a votre nom et a vos regles.',
      features: [
        'Tout le contenu de la formule Professionnelle',
        'Votre logo et vos couleurs',
        'Delais contractuels adaptes par contrat',
        'Hebergement dedie',
        'Reprise de vos donnees existantes',
        'Formation de vos equipes',
      ],
      highlighted: false,
      cta: 'Nous contacter',
    },
  ];

  protected readonly questions: Question[] = [
    {
      q: 'Faut-il installer quelque chose chez mes clients ?',
      a:
        'Non. Depanio fonctionne dans un navigateur, sur ordinateur comme sur ' +
        'telephone. La seule chose a installer physiquement est une etiquette ' +
        'QR autocollante sur les machines, que vous imprimez depuis la ' +
        'plateforme.',
    },
    {
      q: 'Mes clients doivent-ils apprendre a utiliser un logiciel ?',
      a:
        'Un gerant de cafe n a que trois gestes a connaitre : scanner, decrire, ' +
        'confirmer. L interface a ete concue pour quelqu un qui a trente ' +
        'secondes entre deux services, pas pour un informaticien.',
    },
    {
      q: 'Que se passe-t-il si un delai est depasse ?',
      a:
        'Le systeme le constate seul, marque le ticket, previent les ' +
        'responsables et inscrit l evenement dans l historique. Vous savez donc ' +
        'ou vous en etes avant que le client ne vous appelle pour s en plaindre.',
    },
    {
      q: 'Mes donnees et celles de mes clients sont-elles separees ?',
      a:
        'Chaque etablissement ne voit que ses propres tickets, son propre ' +
        'materiel et sa propre fiche. Cette separation est appliquee par le ' +
        'serveur a chaque requete, pas seulement masquee dans l affichage.',
    },
    {
      q: 'Puis-je recuperer mes donnees si j arrete ?',
      a:
        'Oui. Vos donnees vous appartiennent et restent exportables a tout ' +
        'moment, sans frais et sans delai.',
    },
  ];

  protected toggleQuestion(index: number): void {
    this.openQuestion.update((current) => (current === index ? null : index));
  }
}
