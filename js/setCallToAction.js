const ctaButton = document.querySelector('.jumbotron .container p.text-center a');
if (ctaButton) {
  ctaButton.innerHTML = 'View Documentation';
  ctaButton.setAttribute('href', 'docs');
}
